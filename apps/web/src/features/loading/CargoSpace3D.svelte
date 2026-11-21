
<script lang="ts">
    import { onMount } from 'svelte'
    import * as THREE from 'three'
    import { OrbitControls } from 'three/addons/controls/OrbitControls.js'
    import type { CargoPlacement3D } from '../../lib/models/cargoPlacement'

    type Props = {
        lengthMm: number
        widthMm: number
        heightMm: number
        placements?: CargoPlacement3D[]
    }

    let {
        lengthMm,
        widthMm,
        heightMm,
        placements = []
    }: Props = $props()

    let container: HTMLDivElement

    onMount(() => {
        // Convert CHARRON millimeters to Three.js meters.
        const length = lengthMm / 1000
        const width = widthMm / 1000
        const height = heightMm / 1000

        const scene = new THREE.Scene()
        scene.background = new THREE.Color('#101827')

        // Camera
        const camera = new THREE.PerspectiveCamera(
            45,
            1,
            0.1,
            1000
        )

        const center = new THREE.Vector3(
            length / 2,
            height / 2,
            width / 2
        )

        const boundingRadius = Math.sqrt(
            length * length +
            height * height +
            width * width
        ) / 2

        const fovRadians = THREE.MathUtils.degToRad(camera.fov)

        const distance =
            (boundingRadius / Math.sin(fovRadians / 2)) * 1.4

        const viewingDirection = new THREE.Vector3(
            1,
            0.7,
            1
        ).normalize()

        camera.position.copy(
            center.clone().addScaledVector(
                viewingDirection,
                distance
            )
        )

        // Renderer
        const renderer = new THREE.WebGLRenderer({
            antialias: true
        })

        renderer.setPixelRatio(
            Math.min(window.devicePixelRatio, 2)
        )

        renderer.domElement.style.display = 'block'
        renderer.domElement.style.width = '100%'
        renderer.domElement.style.height = '100%'

        container.appendChild(renderer.domElement)

        // Orbit controls
        const controls = new OrbitControls(
            camera,
            renderer.domElement
        )

        controls.enableDamping = true
        controls.dampingFactor = 0.08
        controls.target.copy(center)
        controls.update()

        // Trailer floor
        const floorGeometry = new THREE.BoxGeometry(
            length,
            0.04,
            width
        )

        const floorMaterial = new THREE.MeshStandardMaterial({
            color: '#334155',
            roughness: 0.85
        })

        const floor = new THREE.Mesh(
            floorGeometry,
            floorMaterial
        )

        floor.position.set(
            length / 2,
            -0.02,
            width / 2
        )

        scene.add(floor)

        // Trailer cargo-space outline
        const spaceGeometry = new THREE.BoxGeometry(
            length,
            height,
            width
        )

        const edges = new THREE.EdgesGeometry(spaceGeometry)

        const outlineMaterial = new THREE.LineBasicMaterial({
            color: '#38bdf8'
        })

        const outline = new THREE.LineSegments(
            edges,
            outlineMaterial
        )

        outline.position.set(
            length / 2,
            height / 2,
            width / 2
        )

        scene.add(outline)

        // Cargo units
        const cargoMeshes: THREE.Mesh[] = []

        for (const placement of placements) {
            const rotated = placement.orientation === 'ROTATED_90'

            const cargoLength = (
                rotated ? placement.widthMm : placement.lengthMm
            ) / 1000

            const cargoWidth = (
                rotated ? placement.lengthMm : placement.widthMm
            ) / 1000

            const cargoHeight = placement.heightMm / 1000

            const geometry = new THREE.BoxGeometry(
                cargoLength,
                cargoHeight,
                cargoWidth
            )

            const material = new THREE.MeshStandardMaterial({
                color: '#10b981',
                roughness: 0.7
            })

            const mesh = new THREE.Mesh(geometry, material)

            // CHARRON coordinates:
            // X -> Three.js X (length)
            // Y -> Three.js Z (width)
            // Z -> Three.js Y (height)
            //
            // Convert minimum-corner placement to mesh center.
            mesh.position.set(
                placement.xMm / 1000 + cargoLength / 2,
                placement.zMm / 1000 + cargoHeight / 2,
                placement.yMm / 1000 + cargoWidth / 2
            )

            const boxEdges = new THREE.EdgesGeometry(geometry)

            const edgeMaterial = new THREE.LineBasicMaterial({
                color: '#064e3b'
            })

            const boxOutline = new THREE.LineSegments(
                boxEdges,
                edgeMaterial
            )

            // Make the outline follow the cargo mesh.
            mesh.add(boxOutline)


            scene.add(mesh)
            cargoMeshes.push(mesh)
        }

        // Lighting
        const ambientLight = new THREE.AmbientLight(
            '#ffffff',
            1.5
        )

        scene.add(ambientLight)

        const directionalLight = new THREE.DirectionalLight(
            '#ffffff',
            2
        )

        directionalLight.position.set(5, 10, 7)
        scene.add(directionalLight)

        // Responsive sizing
        function resize() {
            const w = container.clientWidth
            const h = container.clientHeight

            if (w === 0 || h === 0) return

            camera.aspect = w / h
            camera.updateProjectionMatrix()

            renderer.setSize(w, h, false)
        }

        const resizeObserver = new ResizeObserver(resize)
        resizeObserver.observe(container)

        resize()

        // Animation
        let animationId = 0
        let disposed = false

        function animate() {
            if (disposed) return

            animationId = requestAnimationFrame(animate)

            controls.update()
            renderer.render(scene, camera)
        }

        animate()

        // Cleanup only when Svelte unmounts this component.
        return () => {
            disposed = true
            cancelAnimationFrame(animationId)

            resizeObserver.disconnect()
            controls.dispose()


            for (const mesh of cargoMeshes) {
                scene.remove(mesh)

                for (const child of mesh.children) {
                    if (child instanceof THREE.LineSegments) {
                        child.geometry.dispose()

                        const material = child.material as THREE.Material
                        material.dispose()
                    }
                }

                mesh.geometry.dispose()

                const material = mesh.material as THREE.Material
                material.dispose()
            }


            floorGeometry.dispose()
            floorMaterial.dispose()

            spaceGeometry.dispose()
            edges.dispose()
            outlineMaterial.dispose()

            renderer.dispose()
            renderer.domElement.remove()
        }
    })
</script>

<div class="cargo-viewer" bind:this={container}></div>

<style>
    .cargo-viewer {
        width: 100%;
        height: 500px;
        min-height: 350px;
        overflow: hidden;
        border-radius: 12px;
        background: #101827;
    }
</style>

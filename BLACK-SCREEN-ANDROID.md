# Black screen on Android (PowerVR GPU) — findings and blockers

Working document. The full context, diagnostic files, and session diffs are kept **locally**, outside
git, in `game/pantalla-negra-android/` (the `game/` folder is ignored by `.gitignore`).

## Symptom

On a phone with a **PowerVR Rogue GE8320** (MediaTek MT6765): the game **starts with audio and
responds to touches, but the screen stays black**. On PC (NVIDIA) and on another Android (Mali) it
displays fine.

## Conclusion

On this GPU the engine **does not rasterize the geometry**: the vertex/attribute stage produces no
output, even though **all of the render state is correct**. The other hypotheses were ruled out with
tests (see below).

## What was ruled out (with proof)

- **The blit/present is not the cause.** `glBlitFramebuffer` copies nothing on this PowerVR, but
  replacing it by drawing the texture as a quad (with the engine's shader and with direct GL) doesn't
  show either.
- The offscreen FBO is complete (`0x8cd5`); destination and window/scale are correct.
- The **uniform block (`std140`) is not the cause**: delivering the matrix via a normal `uniform`
  doesn't paint either.
- The scissor/viewport/depth/blend are correct.

## What was discovered (measured from the engine)

The data was dumped to an adb-readable file
(`/sdcard/Android/data/com.klisee.reloaded/files/diag.txt`), because `eprintln` is lost in logcat
here.

- The game **does draw to the correct offscreen** (`fb_draw=0x6 = offscreen_fbo`), but the offscreen
  **reads pure black** (`[0,0,0,255]`).
- The draws **run with no GL errors** (e.g. 2040 vertices).
- The **matrices** are correct (ortho 2/320, 2/180).
- The **attributes** are mapped correctly and present in the draw: `vao=1 array_buf=1 prog=3`.
- The **atlases have real data** (4096² RGBA; texel `[131,131,148,255]`).
- The vertex blend color is white `[1,1,1,1]`.
- **Decisive test:** with the fragment shader emitting a **fixed color (magenta)**, the offscreen
  **is still black** → **not a single fragment is rasterized**.

## Real bugs found along the way (still to fix)

1. **`std140` misaligned:** the shader block measures 592 and the Rust `RenderState` struct 716
   (`fog_colour` vec4 must align to 16: offset 576 in GLSL vs 564 in Rust).
2. **Bool shifted:** the shader has `alpha_test` between `lerp` and `fog_enabled`; Rust puts
   `depth_test` there instead.

## Current blocker

We can't keep diagnosing blind and remote without forcing a compile/install/boot cycle (~90 s per
attempt on this phone, and boot is heavy). **Next concrete test:** a shader that generates a
full-screen triangle **without attributes** (position via `gl_VertexID`) with a flat color. If that
paints, it confirms PowerVR isn't delivering vertex attributes; the fix would be to re-establish
attributes/VAO in standard GLES, conditioning it on PowerVR if needed, so as **not to break
NVIDIA/Mali**.

## Aside (performance)

Boot takes ~85–90 s on this phone because the engine uploads the atlases to the GPU **sprite by
sprite** (`push_atlases`). A clear candidate to optimize.

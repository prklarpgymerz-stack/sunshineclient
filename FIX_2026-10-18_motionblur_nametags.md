# Fix: motion blur black screen + self nametags (UNTESTED in game)

MotionBlurRenderer.java  - rewritten from the compiled jar (same 26.2 API calls: gameRenderer.mainRenderTarget(),
                           getColorTexture(), GlTexture.glId()). Adds: sampler-object unbind, texture mip level 0,
                           scissor/stencil/colormask/sRGB neutralised, alpha forced to 1, FBO + GL error checks
                           (prints "[Sunshine] Motion blur disabled: ..." and turns itself off on any problem).
MotionBlurMixin.java     - hooks GameRenderer.renderLevel TAIL (matches the jar; old zip source used "render").
PlayerNametagMixin.java  - other players now real YELLOW (was GOLD); Prosek_MC stays RED.
LivingNametagMixin.java  - NEW. Player renderers override shouldShowName(), so the EntityRenderer hook never ran.
                           require=0, so it is skipped rather than crashing if the signature differs.
sunshine.mixins.json     - registers LivingNametagMixin.

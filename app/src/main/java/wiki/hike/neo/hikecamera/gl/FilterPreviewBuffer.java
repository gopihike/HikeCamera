package wiki.hike.neo.hikecamera.gl;

/**
 * Created by Neo on 10/10/17.
 */

public class FilterPreviewBuffer extends Filter {

    public static final String YUV_VS= "" +
            //"uniform mat4 u_MVPMatrix;\n" +
            "attribute vec4 a_position;\n" +
            "attribute vec2 a_texcoord;\n" +
            "varying vec2 v_texcoord;\n" +
            "void main() {\n" +
            //    "gl_Position = u_MVPMatrix * a_position;\n"+
            "gl_Position =  a_position;\n" +
            "v_texcoord = a_texcoord;\n" +
            "}";

    public static final String YUV_FS = "" +
            "precision highp float;\n" +
            "varying highp vec2 v_texcoord;\n" +
            "uniform sampler2D luminanceTexture;" +
            "uniform sampler2D chrominanceTexture;" +
            "void main() {\n" +
            "   lowp float y = texture2D(luminanceTexture, v_texcoord).r;" +
            "   lowp vec4 uv = texture2D(chrominanceTexture, v_texcoord);" +
            "   mediump vec4 rgba = y * vec4(1.0, 1.0, 1.0, 1.0) + " +
            "                  (uv.a - 0.5) * vec4(0.0, -0.337633, 1.732446, 0.0) + " +
            "                  (uv.r - 0.5) * vec4(1.370705, -0.698001, 0.0, 0.0); " +
            "	gl_FragColor = rgba;" +
            "}";

    /*public static final String YUV_FS = "" +
            "precision highp float;\n" +
            "varying highp vec2 v_texcoord;\n" +
            "uniform sampler2D luminanceTexture;" +
            "uniform sampler2D chrominanceTexture;" +
            "void main() {\n" +
            "   lowp float y = texture2D(luminanceTexture, v_texcoord).r;" +
            "   lowp vec4 uv = texture2D(chrominanceTexture, v_texcoord);" +
            "   mediump vec4 rgba = y * vec4(1.0, 1.0, 1.0, 1.0)" +
            "	gl_FragColor = rgba;"+
            "}";*/

    public FilterPreviewBuffer()
    {
        super(YUV_VS,YUV_FS);
        mRenderType = RENDER_TYPE_PREVIEW_BUFFER;
    }

    @Override
    public void onInit() {
        super.onInit();

    }

    @Override
    public void onInitialized() {
        super.onInitialized();
    }

}

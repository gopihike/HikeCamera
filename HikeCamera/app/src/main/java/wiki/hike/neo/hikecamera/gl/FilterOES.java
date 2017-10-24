package wiki.hike.neo.hikecamera.gl;

/**
 * Created by Neo on 10/10/17.
 */

public class FilterOES extends Filter{

    public static final String NO_FILTER_VERTEX_SHADER= "" +
            //"uniform mat4 u_MVPMatrix;\n" +
            "attribute vec4 a_position;\n" +
            "attribute vec2 a_texcoord;\n" +
            "varying vec2 v_texcoord;\n" +
            "void main() {\n" +
            //    "gl_Position = u_MVPMatrix * a_position;\n"+
            "gl_Position =  a_position;\n" +
            "v_texcoord = a_texcoord;\n" +
            "}";


    public static final String SURFACETEXTURE_OES_FS = "" +
            "#extension GL_OES_EGL_image_external : require\n" +
            "precision mediump float;\n" +
            "uniform lowp samplerExternalOES texSampler;\n" +
            "varying highp vec2 v_texcoord;\n" +
            "void main() {\n" +
            "    vec4 color = texture2D(texSampler, v_texcoord);\n" +
            "    gl_FragColor = color;\n" +
            //"    gl_FragColor = vec4(1.0,0.0,0.0,0.0);\n"+

            "}";

    public FilterOES()
    {
        super(NO_FILTER_VERTEX_SHADER,SURFACETEXTURE_OES_FS);
        mRenderType = RENDER_TYPE_SURFACE_TEXTURE;
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

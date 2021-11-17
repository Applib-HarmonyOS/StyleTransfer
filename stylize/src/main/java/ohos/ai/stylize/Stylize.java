package ohos.ai.stylize;

import ohos.global.resource.RawFileEntry;
import ohos.global.resource.Resource;
import ohos.global.resource.ResourceManager;
import ohos.media.image.ImageSource;
import ohos.media.image.PixelMap;
import ohos.media.image.common.Size;
import org.apache.tvm.Device;
import org.apache.tvm.Function;
import org.apache.tvm.Module;
import org.apache.tvm.NDArray;
import org.apache.tvm.TVMType;
import org.apache.tvm.TVMValue;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.IntBuffer;

/**
 * Main Class for Food Classifier.
 */


public class Stylize {

    private static final String PREDICTION_MODEL_GRAPH_FILE_PATH = "resources/rawfile/prediction/graph.json";
    private static final String TRANSFER_MODEL_GRAPH_FILE_PATH = "resources/rawfile/transfer/graph.json";

    private static final String MODEL_CPU_LIB_FILE_NAME = "deploy_lib.so";

    private static final String PREDICTION_MODEL_CPU_LIB_FILE_PATH = "resources/rawfile/prediction/deploy_lib.so";
    private static final String TRANSFER_MODEL_CPU_LIB_FILE_PATH = "resources/rawfile/transfer/deploy_lib.so";

    private static final String PREDICTION_MODEL_PARAMS_FILE_PATH = "resources/rawfile/prediction/params.bin";
    private static final String TRANSFER_MODEL_PARAMS_FILE_PATH = "resources/rawfile/transfer/params.bin";
    private static final String TAG = Stylize.class.getName();

    // TVM constants
    private int outputIndex = 0;
    private int imgChannel = 3;
    private String inputName = "style_image";
    private String inputNameContent = "content_image";
    private String inputNamePredOutput = "mobilenet_conv/Conv/BiasAdd";
    private int modelInputSize = 256;
    private int contentInputSize = 384;
    private String imagePath;
    private String imageName;
    private String styleImagePath;
    private String styleImageName;
    ResourceManager resManager;
    File cachedir;
    float[] stylePredict = null;
    float[] styleContent = null;

    /**
     * Stylize Constructor.
     *
     * @param path         - path for input image
     * @param name         - name of input image
     * @param styleImgPath - path for input style image
     * @param styleImgName - name of input style image
     * @param resm         - ResourceManager GetResourceManager()
     * @param f            - CacheDir()
     */

    public Stylize(String path, String name, String styleImgPath, String styleImgName, ResourceManager resm, File f) {
        this.imagePath = path;
        this.imageName = name;
        this.styleImagePath = styleImgPath;
        this.styleImageName = styleImgName;
        this.resManager = resm;
        this.cachedir = f;
        run_style_transfer();
    }

    public float[] get_output() {
        return styleContent;
    }

    private void run_prediction() {
        // load json graph
        String modelGraph = null;
        RawFileEntry rawFileEntryModel = resManager.getRawFileEntry(PREDICTION_MODEL_GRAPH_FILE_PATH);
        try {
            modelGraph = new String(getBytesFromRawFile(rawFileEntryModel));
        } catch (IOException e) {
            return; //failure
        }

        // create java tvm device
        Device tvmDev = Device.cpu();

        RawFileEntry rawFileEntryModelLib = resManager.getRawFileEntry(PREDICTION_MODEL_CPU_LIB_FILE_PATH);
        File file = null;
        Module modelLib = null;
        try {
            file = getFileFromRawFile("prediction_" + MODEL_CPU_LIB_FILE_NAME, rawFileEntryModelLib, cachedir);
            modelLib = Module.load(file.getAbsolutePath());
        } catch (NullPointerException | IOException e) {
            e.printStackTrace();
        }

        Function runtimeCreFun = Function.getFunction("tvm.graph_executor.create");
        TVMValue runtimeCreFunRes = runtimeCreFun.pushArg(modelGraph)
                .pushArg(modelLib)
                .pushArg(tvmDev.deviceType)
                .pushArg(tvmDev.deviceId)
                .invoke();
        Module graphExecutorModule = runtimeCreFunRes.asModule();

        // load parameters
        byte[] modelParams = null;
        RawFileEntry rawFileEntryModelParams = resManager.getRawFileEntry(PREDICTION_MODEL_PARAMS_FILE_PATH);
        try {
            modelParams = getBytesFromRawFile(rawFileEntryModelParams);
        } catch (IOException e) {
            e.printStackTrace();
            return; //failure
        }

        // get the function from the module(load parameters)
        Function loadParamFunc = graphExecutorModule.getFunction("load_params");
        loadParamFunc.pushArg(modelParams).invoke();

        RawFileEntry rawFileEntryImage = resManager.getRawFileEntry(styleImagePath);
        File fileImage = null;
        try {
            fileImage = getFileFromRawFile(styleImageName, rawFileEntryImage, cachedir);
        } catch (IOException e) {
            e.printStackTrace();
        }

        ImageSource imageSource = ImageSource.create(fileImage, null);
        ImageSource.DecodingOptions decodingOpts = new ImageSource.DecodingOptions();
        decodingOpts.desiredSize = new Size(modelInputSize, modelInputSize);
        PixelMap pixelMap = imageSource.createPixelmap(decodingOpts);

        IntBuffer imageBuffer = IntBuffer.allocate(pixelMap.getImageInfo().size.height
                * pixelMap.getImageInfo().size.width);
        pixelMap.readPixels(imageBuffer);

        // image RGB values
        float[] imgRgbValues = new float[modelInputSize * modelInputSize * imgChannel];

        // image pixel int values
        int[] pixelValues = imageBuffer.array();

        for (int j = 0; j < pixelValues.length; ++j) {
            imgRgbValues[j * 3 + 0] = ((pixelValues[j] >> 16) & 0xFF) / 255.0f;
            imgRgbValues[j * 3 + 1] = ((pixelValues[j] >> 8) & 0xFF) / 255.0f;
            imgRgbValues[j * 3 + 2] = (pixelValues[j] & 0xFF) / 255.0f;
        }

        // get the function from the module(set input data)
        NDArray inputNdArray = NDArray.empty(new long[]{1,
            modelInputSize, modelInputSize, imgChannel}, new TVMType("float32"));

        inputNdArray.copyFrom(imgRgbValues);
        Function setInputFunc = graphExecutorModule.getFunction("set_input");
        setInputFunc.pushArg(inputName).pushArg(inputNdArray).invoke();
        // release tvm local variables
        inputNdArray.release();
        setInputFunc.release();

        // get the function from the module(run it)
        Function runFunc = graphExecutorModule.getFunction("run");
        runFunc.invoke();
        // release tvm local variables
        runFunc.release();

        // get the function from the module(get output data)
        NDArray outputNdArray = NDArray.empty(new long[]{1, 1, 1, 100}, new TVMType("float32"));
        Function getOutputFunc = graphExecutorModule.getFunction("get_output");
        getOutputFunc.pushArg(outputIndex).pushArg(outputNdArray).invoke();
        float[] output = outputNdArray.asFloatArray();
        // release tvm local variables
        outputNdArray.release();
        getOutputFunc.release();

        this.stylePredict = output;
    }

    private void run_stylize_content() {
        // load json graph
        String modelGraph = null;
        RawFileEntry rawFileEntryModel = resManager.getRawFileEntry(TRANSFER_MODEL_GRAPH_FILE_PATH);
        try {
            modelGraph = new String(getBytesFromRawFile(rawFileEntryModel));
        } catch (IOException e) {
            return; //failure
        }

        // create java tvm device
        Device tvmDev = Device.cpu();

        RawFileEntry rawFileEntryModelLib = resManager.getRawFileEntry(TRANSFER_MODEL_CPU_LIB_FILE_PATH);
        File file = null;
        Module modelLib = null;
        try {
            file = getFileFromRawFile("tranfer_" + MODEL_CPU_LIB_FILE_NAME, rawFileEntryModelLib, cachedir);
            modelLib = Module.load(file.getAbsolutePath());
        } catch (NullPointerException | IOException e) {
            e.printStackTrace();
        }

        Function runtimeCreFun = Function.getFunction("tvm.graph_executor.create");
        TVMValue runtimeCreFunRes = runtimeCreFun.pushArg(modelGraph)
                .pushArg(modelLib)
                .pushArg(tvmDev.deviceType)
                .pushArg(tvmDev.deviceId)
                .invoke();
        Module graphExecutorModule = runtimeCreFunRes.asModule();

        // load parameters
        byte[] modelParams = null;
        RawFileEntry rawFileEntryModelParams = resManager.getRawFileEntry(TRANSFER_MODEL_PARAMS_FILE_PATH);
        try {
            modelParams = getBytesFromRawFile(rawFileEntryModelParams);
        } catch (IOException e) {
            e.printStackTrace();
            return; //failure
        }

        // get the function from the module(load parameters)
        Function loadParamFunc = graphExecutorModule.getFunction("load_params");
        loadParamFunc.pushArg(modelParams).invoke();

        RawFileEntry rawFileEntryImage = resManager.getRawFileEntry(imagePath);
        File fileImage = null;
        try {
            fileImage = getFileFromRawFile(imageName, rawFileEntryImage, cachedir);
        } catch (IOException e) {
            e.printStackTrace();
        }

        ImageSource imageSource = ImageSource.create(fileImage, null);
        ImageSource.DecodingOptions decodingOpts = new ImageSource.DecodingOptions();
        decodingOpts.desiredSize = new Size(contentInputSize, contentInputSize);
        PixelMap pixelMap = imageSource.createPixelmap(decodingOpts);

        IntBuffer imageBuffer = IntBuffer.allocate(pixelMap.getImageInfo().size.height
                * pixelMap.getImageInfo().size.width);
        pixelMap.readPixels(imageBuffer);

        // image RGB values
        float[] imgRgbValues = new float[contentInputSize * contentInputSize * imgChannel];

        // image pixel int values
        int[] pixelValues = imageBuffer.array();

        for (int j = 0; j < pixelValues.length; ++j) {
            imgRgbValues[j * 3 + 0] = ((pixelValues[j] >> 16) & 0xFF) / 255.0f;
            imgRgbValues[j * 3 + 1] = ((pixelValues[j] >> 8) & 0xFF) / 255.0f;
            imgRgbValues[j * 3 + 2] = (pixelValues[j] & 0xFF) / 255.0f;
        }

        // get the function from the module(set input data)
        NDArray inputNdArray = NDArray.empty(new long[]{1,
            contentInputSize, contentInputSize, imgChannel}, new TVMType("float32"));
        inputNdArray.copyFrom(imgRgbValues);

        NDArray predInputNdArray = NDArray.empty(new long[]{1, 1, 1, 100}, new TVMType("float32"));
        predInputNdArray.copyFrom(stylePredict);

        Function setInputFunc = graphExecutorModule.getFunction("set_input");
        setInputFunc.pushArg(inputNameContent).pushArg(inputNdArray).invoke();
        setInputFunc.pushArg(inputNamePredOutput).pushArg(predInputNdArray).invoke();
        // release tvm local variables
        inputNdArray.release();
        predInputNdArray.release();
        setInputFunc.release();

        // get the function from the module(run it)
        Function runFunc = graphExecutorModule.getFunction("run");
        runFunc.invoke();
        // release tvm local variables
        runFunc.release();

        // get the function from the module(get output data)
        NDArray outputNdArray = NDArray.empty(new long[]{1,
            contentInputSize, contentInputSize, imgChannel}, new TVMType("float32"));
        Function getOutputFunc = graphExecutorModule.getFunction("get_output");
        getOutputFunc.pushArg(outputIndex).pushArg(outputNdArray).invoke();
        float[] output = outputNdArray.asFloatArray();
        // release tvm local variables
        outputNdArray.release();
        getOutputFunc.release();

        this.styleContent = output;
    }

    /**
     * Main Function to run food classifier.
     */
    public void run_style_transfer() {

        // First execute prediction
        run_prediction();

        // Post execute stylize content
        run_stylize_content();
    }

    private static File getFileFromRawFile(String filename, RawFileEntry rawFileEntry, File cacheDir)
            throws IOException {
        byte[] buf = null;
        File file;
        file = new File(cacheDir, filename);
        try (FileOutputStream output = new FileOutputStream(file)) {
            Resource resource = rawFileEntry.openRawFile();
            buf = new byte[(int) rawFileEntry.openRawFileDescriptor().getFileSize()];
            int bytesRead = resource.read(buf);
            if (bytesRead != buf.length) {
                throw new IOException("Asset Read failed!!!");
            }
            output.write(buf, 0, bytesRead);
            return file;
        }
    }

    private static byte[] getBytesFromRawFile(RawFileEntry rawFileEntry)
            throws IOException {
        byte[] buf = null;
        try {
            Resource resource = rawFileEntry.openRawFile();
            buf = new byte[(int) rawFileEntry.openRawFileDescriptor().getFileSize()];
            int bytesRead = resource.read(buf);
            if (bytesRead != buf.length) {
                throw new IOException("Asset Read failed!!!");
            }
            return buf;
        } catch (IOException ex) {
            throw new IllegalStateException(ex);
        }
    }
}

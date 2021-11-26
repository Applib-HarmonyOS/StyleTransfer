package ohos.ai.styletransfer;

import ohos.aafwk.ability.delegation.AbilityDelegatorRegistry;
import ohos.ai.stylize.Stylize;
import ohos.app.Context;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class ExampleOhosTest {
    private static String imgPath = "entry/resources/rawfile/belfry-2611573_1280.jpg";
    private static String imgName = "belfry-2611573_1280.jpg";
    private static String styleImgPath = "entry/resources/rawfile/style23.jpg";
    private static String styleImgName = "style23.jpg";

    @Test
    public void test() {

        Context mContext = AbilityDelegatorRegistry.getAbilityDelegator().getAppContext();

        Stylize mtestclassifier = new Stylize(imgPath, imgName,
                styleImgPath, styleImgName, mContext.getResourceManager(), mContext.getCacheDir());

        float[] output = mtestclassifier.getOutput();

        assertEquals(384 * 384 * 3, output.length);
    }
}

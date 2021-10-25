package ohos.ai.styletransfer;

import ohos.aafwk.ability.delegation.AbilityDelegatorRegistry;
import ohos.ai.stylize.Stylize;
import ohos.app.Context;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class ExampleOhosTest {
    private static String img_path = "entry/resources/rawfile/belfry-2611573_1280.jpg";
    private static String img_name = "belfry-2611573_1280.jpg";
    private static String styleImg_path = "entry/resources/rawfile/style23.jpg";
    private static String styleImg_name = "style23.jpg";
    private Context mContext;
    private Stylize mtestclassifier;

    @Test
    public void test() {

        mContext = AbilityDelegatorRegistry.getAbilityDelegator().getAppContext();

        mtestclassifier = new Stylize(img_path, img_name,
                styleImg_path, styleImg_name, mContext.getResourceManager(), mContext.getCacheDir());

        float[] output = mtestclassifier.get_output();

        assertEquals(384 * 384 * 3, output.length);
    }
}
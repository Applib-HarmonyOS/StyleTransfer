package ohos.ai.styletransfer.slice;

import ohos.aafwk.ability.AbilitySlice;
import ohos.aafwk.content.Intent;
import ohos.ai.styletransfer.ResourceTable;
import ohos.ai.stylize.Stylize;
import org.apache.tvm.LogUtil;

public class MainAbilitySlice extends AbilitySlice {
    private static final String TAG = MainAbilitySlice.class.getName();

    @Override
    public void onStart(Intent intent) {
        super.onStart(intent);
        String imgPath = "entry/resources/rawfile/belfry-2611573_1280.jpg";
        String imgName = "belfry-2611573_1280.jpg";
        String styleImgPath = "entry/resources/rawfile/style23.jpg";
        String styleImgName = "style23.jpg";

        Stylize mtestclassifier = new Stylize(imgPath, imgName,
                styleImgPath, styleImgName, getResourceManager(), getCacheDir());

        float[] output = mtestclassifier.get_output();
        LogUtil.info(TAG, "prediction finished");

        super.setUIContent(ResourceTable.Layout_ability_main);
    }

    @Override
    public void onActive() {
        super.onActive();
    }

    @Override
    public void onForeground(Intent intent) {
        super.onForeground(intent);
    }
}

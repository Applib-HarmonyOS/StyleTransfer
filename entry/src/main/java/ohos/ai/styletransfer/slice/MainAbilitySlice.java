package ohos.ai.styletransfer.slice;

import ohos.aafwk.ability.AbilitySlice;
import ohos.aafwk.content.Intent;
import ohos.ai.styletransfer.ResourceTable;
import ohos.ai.stylize.Stylize;

public class MainAbilitySlice extends AbilitySlice {

    @Override
    public void onStart(Intent intent) {
        super.onStart(intent);
        String imgPath = "entry/resources/rawfile/belfry-2611573_1280.jpg";
        String imgName = "belfry-2611573_1280.jpg";
        String styleImgPath = "entry/resources/rawfile/style23.jpg";
        String styleImgName = "style23.jpg";

        Stylize mtestclassifier = new Stylize(imgPath, imgName,
                styleImgPath, styleImgName, getResourceManager(), getCacheDir());

        mtestclassifier.getOutput();

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

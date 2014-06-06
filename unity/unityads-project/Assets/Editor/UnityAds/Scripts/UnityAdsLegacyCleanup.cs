using UnityEngine;
using UnityEditor;
using System.IO;
using System.Collections;

public class UnityAdsLegacyCleanup : AssetPostprocessor
{

    private static string[] filesToRemove = {
        "Editor/PostprocessBuildPlayer_ImpactSDK",
        "Editor/UpdateXcodeImpact.pyc",
        "Plugins/Android/applifier-impact-android-unity3d.jar",
        "Plugins/Android/applifier-impact-android.jar",
        "Plugins/Android/gameads",
        "Plugins/ApplifierImpactMobile",
        "Plugins/GameAds",
        "Plugins/iOS/ApplifierImpactUnity3DWrapper.h",
        "Plugins/iOS/ApplifierImpactUnity3DWrapper.mm",
        "Plugins/iOS/Resources/ApplifierImpact.bundle",
        "Plugins/iOS/Resources/ApplifierImpact.framework"
    };

    void OnPreprocessTexture()
    {
        // Legacy clean (moving asset) often fails during package import, try to do it a couple of times pre import and one time post import
        if(assetPath.Contains("Plugins/UnityAds/iOS/UnityAds.bundle")) {
            Clean(true);
        }
    }

    static void OnPostprocessAllAssets(string[] importedAssets, string[] deletedAssets, string[] movedAssets, string[] movedFromAssetPaths)
    {
        // Legacy clean (moving asset) often fails during package import, try to do it a couple of times pre import and one time post import
        foreach(string asset in importedAssets) {
            if(asset.Trim().Equals("Assets/Plugins/UnityAds/UnityAds.prefab") ||
               asset.Trim().Equals("Assets/Plugins/GameAds/GameAds.prefab") ||
               asset.Trim().Equals("Assets/Plugins/ApplifierImpactMobile/ApplifierImpactMobile.prefab")) {
                Clean(true);
                return;
            }
        }
    }

    public static void Clean(bool silenceErrors)
    {
        foreach(string fileName in filesToRemove) {
            if(File.Exists(System.IO.Path.Combine(Application.dataPath, fileName)) ||
               Directory.Exists(System.IO.Path.Combine(Application.dataPath, fileName))) {
                AssetDatabase.DeleteAsset(System.IO.Path.Combine("Assets", fileName));
                Debug.Log("Removed legacy UnityAds asset: " + fileName);
            }
        }
    }
}

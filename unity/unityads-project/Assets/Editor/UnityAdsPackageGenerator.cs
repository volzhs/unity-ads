using UnityEngine;
using UnityEditor;
using System.Collections;

public class UnityAdsPackageGenerator : MonoBehaviour {

	private static string[] assetPathNames = {
		"Assets/Editor/PostprocessBuildPlayer",
		"Assets/Editor/PostprocessBuildPlayer_UnityAdsSDK",
		"Assets/Editor/UpdateXcodeUnityAds.pyc",

		"Assets/Editor/UnityAds",
		"Assets/Plugins/UnityAds",

		"Assets/Plugins/iOS/UnityAdsUnityWrapper.mm",
		"Assets/Plugins/iOS/UnityAdsUnityWrapper.h",

		"Assets/Plugins/Android/unityads"
	};

	private static string defaultFileName = "UnityAds";
	private static string defaultFileExtension = "unitypackage";

	[MenuItem("UnityAds/Create Package")]
	public static void CreatePackageUI() {
		string fileNameWithPath = EditorUtility.SaveFilePanel( "Save UnityAds package", "", defaultFileName + "." + defaultFileExtension, defaultFileExtension);

        if(fileNameWithPath.Length != 0) {
			AssetDatabase.ExportPackage(assetPathNames, fileNameWithPath, ExportPackageOptions.Interactive | ExportPackageOptions.Recurse | ExportPackageOptions.IncludeDependencies);

			Debug.Log ("UnityAds package created.");
		}
	}
	
	public static void CreatePackage() {
        string fileNameWithPath = System.Environment.GetEnvironmentVariable("UNITYADS_UNITY_PACKAGE");

        if(fileNameWithPath == null || fileNameWithPath.Length == 0) {
            fileNameWithPath = defaultFileName + "." + defaultFileExtension;
        }

		AssetDatabase.ExportPackage(assetPathNames, fileNameWithPath, ExportPackageOptions.Recurse | ExportPackageOptions.IncludeDependencies);

		Debug.Log ("UnityAds package created.");
	}
}

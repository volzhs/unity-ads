default: release

generate:
	javac GenerateUnityApi.java
	java GenerateUnityApi
	rm -f *.class

build: generate
	ndk-build clean
	ndk-build
	find ../obj -type d -name objs | xargs rm -rf
	cp src/UnityAdsAndroidBridge.h ../obj/local

release: clean build
	cd ../.. && ./gradlew assembleRelease
	cp ../build/outputs/aar/sources-release.aar ../obj/local/unityads-release.aar
	cd ../obj/local/ && zip -r ../../jni/builds.zip *

clean:
	rm -f *.class
	rm -rf ../obj
	rm -f builds.zip
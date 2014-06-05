#!/bin/sh
date=`date '+%Y%m%d'`

if [ -e UnityAds/UnityAdsProperties/UnityAdsProperties.m ]; then
    version=`grep "kUnityAdsVersion = " UnityAds/UnityAdsProperties/UnityAdsProperties.m | sed 's/[^0-9]*//g'`
	date="$date-$version"
fi

sdk_dir="~/Desktop/unity-ads-builds/ios/$date"
sdk_dir=`eval "echo $sdk_dir"`

rm -rf $sdk_dir
mkdir -p $sdk_dir
(cd $sdk_dir/.. ; ln -sfh $date latest)

sdk_file=$sdk_dir/UnityAds$date.zip

prefix=$(cd "$(dirname "$0")"; pwd)

set -e
set -v

cd $prefix

rm -rf build
rm -rf ~/Library/Developer/Xcode/DerivedData/UnityAds*

xcodebuild -project UnityAds.xcodeproj -configuration Release

cd $prefix/build/Release-iphoneos

files="UnityAds.bundle UnityAds.framework"

zip -9 -r -y $sdk_file $files
cp -a $files $sdk_dir

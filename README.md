# Go-Ubiquitous
<img src="../wear/src/main/res/drawable/interactive.png" width="315" height="315 &nbsp"/>
<img src="../wear/src/main/res/drawable/Ambient.png" width="315" height="315"/>

## Project Overview
In this project, you will build a wearable watch face for Sunshine to run on an Android Wear device.

## Setup
This project requires an Open Weather Map API key, which you can obtain from [Open Weather Map site](https://openweathermap.org/api). After you obtain an Open Weather Map key enter it in the App build.gradle file at:
<pre><code>
android {
   ...
    buildTypes.each {
        it.buildConfigField 'String', 'OPEN_WEATHER_MAP_API_KEY', <b>'"Place your Open Weather Map Key here"'</b>
    }
}
</code></pre>
<b>Note:</b> Place key inside the double quotes. 

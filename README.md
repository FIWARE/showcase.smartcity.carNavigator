# FIWARE-Enabled Car Navigator for smart cities

[![GPL license][license-image]][license-url] ![](https://nexus.lab.fiware.org/static/badges/statuses/deprecated.svg)
[![Support badge](https://img.shields.io/badge/tag-fiware-orange.svg?logo=stackoverflow)](https://stackoverflow.com/questions/tagged/fiware)

## Introduction

This repository contains code which implemented an Android-based car navigator for smart cities . 
The application connects [HERE Maps](https://maps.here.com/)
technology with [OASC cities](http://oascities.org/) which expose real time data through the
[FIWARE-NGSI APIs](http://fiware.github.io/context.Orion/api/v2/).

This pilot was presented at the
[SCEWC 2015](https://www.fiware.org/2015/11/20/porto-a-city-that-has-become-a-real-time-guide/) and
[MWC 2016](http://www.gsma.com/connectedliving/wp-content/uploads/2016/01/MWC16-Conn-Living-Guide-web.pdf).

## Showcase description

This showcase demonstrates how car navigation can be enriched by means of real time context data
from smart cities. A smarter car navigator prototype, integrating both, real-time context data coming from several cities
(Spain, Portugal and The Netherlands) and HERE maps is shown.

Two different use cases are demonstrated: 

* Car navigation experience on route is enriched by providing real time environmental
information about the city and its facilities. Drivers know real time ambient data and can be advised,
for instance of high pollution levels, influencing their driving behavior. 

* Drivers are guided to a suitable parking spot (indoors or outdoors),
depending on different parameters such as vehicle type, traffic incidences, occupancy levels of nearby parkings and the like.

A video of the functionality offered can be found [here](https://drive.google.com/file/d/0ByPJ3uXnTexAM2t5SGNpUEFtblk/view).

## How to run it

The application runs on top of the
[HERE Maps Premium SDK](https://developer.here.com/mobile-sdks/documentation/android-hybrid-plus/topics/overview.html) for Android.
So in order to run it you would need to apply for an evaluation copy of such SDK and register an application under the namespace
`fiware.smartcity`. Then you will need to configure the corresponding credentials in the `AndroidManifest.xml`file.

For your convenience an example of manifest.xml file is provided, so that you will only
have to add your credentials at the corresponding `meta-data` sections. 

## How to contribute

First of all, please take into account that the repository contains a snapshot ready to work with [Android Studio](http://developer.android.com/intl/es/tools/studio/index.html). 

Once you have a patch please make a pull request.

[license-image]: https://img.shields.io/badge/license-GPL-blue.svg
[license-url]: LICENSE

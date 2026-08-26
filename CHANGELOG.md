# [2.6.0](https://github.com/OneLiteFeatherNET/Cygnus/compare/v2.5.1...v2.6.0) (2026-06-28)


### Features

* **setup:** add support for data deletion ([#105](https://github.com/OneLiteFeatherNET/Cygnus/issues/105)) ([92670e5](https://github.com/OneLiteFeatherNET/Cygnus/commit/92670e5258e906c5c36e515185bebd678be9ba3a))

## [2.7.2](https://github.com/OneLiteFeatherNET/Cygnus/compare/v2.7.1...v2.7.2) (2026-08-26)


### ⚠ BREAKING CHANGES

* **setup:** update SetupItems class to a full utility class

### Features

* add Cloudnet integration ([#19](https://github.com/OneLiteFeatherNET/Cygnus/issues/19)) ([4d3ff9e](https://github.com/OneLiteFeatherNET/Cygnus/commit/4d3ff9e718839b05d84b0269790fbce30ddf1ba4))
* add dialog base for the setup input ([#69](https://github.com/OneLiteFeatherNET/Cygnus/issues/69)) ([c8b2bfb](https://github.com/OneLiteFeatherNET/Cygnus/commit/c8b2bfbe8445f2bdd186acfa9d30b1bae0a596ba))
* add readme file ([5c3376e](https://github.com/OneLiteFeatherNET/Cygnus/commit/5c3376eb7fa830001e0f7f982a50e3bfd9089d50))
* **build:** add CycloneDX plugin for dependency tracking ([cda6029](https://github.com/OneLiteFeatherNET/Cygnus/commit/cda6029fdf2991992e698220dd0a9187da3f0704))
* **build:** update publishing configuration and remove deprecated publishData plugin ([b79a07e](https://github.com/OneLiteFeatherNET/Cygnus/commit/b79a07eb688f0830393cddb12b6aee5f1b2b648a))
* **common:** add basic block handlers ([#197](https://github.com/OneLiteFeatherNET/Cygnus/issues/197)) ([33b3e48](https://github.com/OneLiteFeatherNET/Cygnus/commit/33b3e48108eeb4e8c94561ef157e14f0ebc491ee))
* **common:** add custom dimension presets ([#132](https://github.com/OneLiteFeatherNET/Cygnus/issues/132)) ([2e5482d](https://github.com/OneLiteFeatherNET/Cygnus/commit/2e5482dde4871dc491f09695976dca6d7a529429))
* **game:** add ability to use a custom page model ([#133](https://github.com/OneLiteFeatherNET/Cygnus/issues/133)) ([c069dbb](https://github.com/OneLiteFeatherNET/Cygnus/commit/c069dbb5bf73b9bd3eee2ef53721f11c2e9a3162))
* **game:** Add game configuration, view interfaces, and Docker setup ([c65313e](https://github.com/OneLiteFeatherNET/Cygnus/commit/c65313e43fd9fb7c6c41a9d52ae81b64d6f7e5fa))
* **game:** add mannequin entity ([#150](https://github.com/OneLiteFeatherNET/Cygnus/issues/150)) ([c86bffb](https://github.com/OneLiteFeatherNET/Cygnus/commit/c86bffb9a61ac17d8dc698777ae397416baec7dd))
* **game:** add player heartbeat ([#147](https://github.com/OneLiteFeatherNET/Cygnus/issues/147)) ([7c8f7f0](https://github.com/OneLiteFeatherNET/Cygnus/commit/7c8f7f056779896bcb0102c9f5ada94fa851e9e0))
* **game:** transition to night at the end of the lobby phase ([#165](https://github.com/OneLiteFeatherNET/Cygnus/issues/165)) ([8766880](https://github.com/OneLiteFeatherNET/Cygnus/commit/8766880f39b27eeac6be3b6f24c72e172abdcc12))
* introduce new setup data implementations ([#73](https://github.com/OneLiteFeatherNET/Cygnus/issues/73)) ([a4d7b9f](https://github.com/OneLiteFeatherNET/Cygnus/commit/a4d7b9fbd9675554678f48033cb87f04995cce37))
* **map:** add map announcement ([#171](https://github.com/OneLiteFeatherNET/Cygnus/issues/171)) ([3ed6b4d](https://github.com/OneLiteFeatherNET/Cygnus/commit/3ed6b4da05fd9f8d5fb19d78ad8cd275a6ab120d))
* **map:** read worlds through Falco instead of the Minestom loader ([#143](https://github.com/OneLiteFeatherNET/Cygnus/issues/143)) ([0c0ef46](https://github.com/OneLiteFeatherNET/Cygnus/commit/0c0ef465208db83838f1d3084caaa63a169388bb))
* **pack:** add resource pack handling ([#164](https://github.com/OneLiteFeatherNET/Cygnus/issues/164)) ([f1b1266](https://github.com/OneLiteFeatherNET/Cygnus/commit/f1b12665b3c70de9a16b0d84450967313d8005b3))
* **phase:** add sounds to the lobby countdown ([#166](https://github.com/OneLiteFeatherNET/Cygnus/issues/166)) ([a282883](https://github.com/OneLiteFeatherNET/Cygnus/commit/a282883bed65a5ffe918e6b9dc31cf141f22b78e))
* **release:** implement semantic release workflow and update build configuration ([66b5f43](https://github.com/OneLiteFeatherNET/Cygnus/commit/66b5f4365eac5f4d3562cb9ee1c70d2ba7fe198e))
* **repo:** add readme file ([#168](https://github.com/OneLiteFeatherNET/Cygnus/issues/168)) ([5c3376e](https://github.com/OneLiteFeatherNET/Cygnus/commit/5c3376eb7fa830001e0f7f982a50e3bfd9089d50))
* run Cygnus without LuckPerms ([#154](https://github.com/OneLiteFeatherNET/Cygnus/issues/154)) ([aa00aa3](https://github.com/OneLiteFeatherNET/Cygnus/commit/aa00aa3b78ec1a7e8d5216e6d4d35565fc646848))
* **setup:** add ability to delete locations in map setup ([#126](https://github.com/OneLiteFeatherNET/Cygnus/issues/126)) ([ea30cce](https://github.com/OneLiteFeatherNET/Cygnus/commit/ea30cced193ba1547d6d27e813b26bd1aa1ff0ac))
* **setup:** add data overview inventory for the setup ([#102](https://github.com/OneLiteFeatherNET/Cygnus/issues/102)) ([c9b0e6a](https://github.com/OneLiteFeatherNET/Cygnus/commit/c9b0e6acec841ffd8c2a84a703bed12efca9826f))
* **setup:** add default lore for fallback items ([#125](https://github.com/OneLiteFeatherNET/Cygnus/issues/125)) ([c2e324a](https://github.com/OneLiteFeatherNET/Cygnus/commit/c2e324a5d11e86e6eac5857e1a9af667d80321cd))
* **setup:** add dialog related code ([#101](https://github.com/OneLiteFeatherNET/Cygnus/issues/101)) ([98b58bd](https://github.com/OneLiteFeatherNET/Cygnus/commit/98b58bdb79045c7792b4f6f2c39dafc4a09bb861))
* **setup:** add page setup part ([#146](https://github.com/OneLiteFeatherNET/Cygnus/issues/146)) ([f035799](https://github.com/OneLiteFeatherNET/Cygnus/commit/f0357996f3ac51a78cf64fde77b1d2d27e9c706c))
* **setup:** add setup player implementation ([#72](https://github.com/OneLiteFeatherNET/Cygnus/issues/72)) ([4f75571](https://github.com/OneLiteFeatherNET/Cygnus/commit/4f75571a61b6944fa4b532887998f48e97ad670a))
* **setup:** add setup player implementation ([#72](https://github.com/OneLiteFeatherNET/Cygnus/issues/72)) ([947cb5b](https://github.com/OneLiteFeatherNET/Cygnus/commit/947cb5bafa48144cbbfeb39de26521a9de2d11f6))
* **setup:** add support for data deletion ([#105](https://github.com/OneLiteFeatherNET/Cygnus/issues/105)) ([92670e5](https://github.com/OneLiteFeatherNET/Cygnus/commit/92670e5258e906c5c36e515185bebd678be9ba3a))
* **setup:** improve setup item display ([#124](https://github.com/OneLiteFeatherNET/Cygnus/issues/124)) ([dd475df](https://github.com/OneLiteFeatherNET/Cygnus/commit/dd475df97e70afe79cf152c182fdc83bcc0374b9))
* **setup:** overhaul dialog data handling ([#128](https://github.com/OneLiteFeatherNET/Cygnus/issues/128)) ([622dcdf](https://github.com/OneLiteFeatherNET/Cygnus/commit/622dcdfd0e8b06afc94bb425dc77ab06578ffa39))
* **setup:** update SetupItems class to a full utility class ([6e77081](https://github.com/OneLiteFeatherNET/Cygnus/commit/6e77081e845d8f208def2102b8a0e05785cae1b4))
* **spectator:** add ability to spectate players ([#159](https://github.com/OneLiteFeatherNET/Cygnus/issues/159)) ([d29578b](https://github.com/OneLiteFeatherNET/Cygnus/commit/d29578b31bb55d6f662421bafa4449e06dd95376))
* **stats:** add simple ingame statistics ([#189](https://github.com/OneLiteFeatherNET/Cygnus/issues/189)) ([3c7b580](https://github.com/OneLiteFeatherNET/Cygnus/commit/3c7b580438ed39b515478ffd62074e0854d740b4))
* **teleport:** add strategy interface ([#135](https://github.com/OneLiteFeatherNET/Cygnus/issues/135)) ([bffa72f](https://github.com/OneLiteFeatherNET/Cygnus/commit/bffa72f0b4ea9992b4375b039af8eb3e7c49477f))


### Bug Fixes

* **ambient:** improve service creation call ([147817e](https://github.com/OneLiteFeatherNET/Cygnus/commit/147817efd5cb8742c82da7d33ced1644414d90a8))
* **ambient:** use survivor team ([75aff1a](https://github.com/OneLiteFeatherNET/Cygnus/commit/75aff1aaf42b6092456062be87b6080c91739bd3))
* **attribute:** update movement attribute call ([84b66af](https://github.com/OneLiteFeatherNET/Cygnus/commit/84b66aff61061c06651cdefab6d407b05f666556))
* **build:** simplify version extraction logic in build.gradle.kts ([#117](https://github.com/OneLiteFeatherNET/Cygnus/issues/117)) ([43b87f3](https://github.com/OneLiteFeatherNET/Cygnus/commit/43b87f3096a8d5bc667ce3d109255f340b85e806))
* **category:** avoid typo in one enum entry declaration ([b79d4e6](https://github.com/OneLiteFeatherNET/Cygnus/commit/b79d4e69b4e051732632eb9d85162494207eb67e))
* **chat:** fix component handling ([e1d6617](https://github.com/OneLiteFeatherNET/Cygnus/commit/e1d6617e9b618c49531b3e03a1d6d16bf2438c67))
* **ci:** trigger build ([21400f2](https://github.com/OneLiteFeatherNET/Cygnus/commit/21400f28bfc635ff1540e5932c256aeff32b016a))
* **ci:** trigger build ([fbdb09c](https://github.com/OneLiteFeatherNET/Cygnus/commit/fbdb09c2e233d993739820515ab8ad57ecc691fa))
* **ci:** trigger build ([edb6f1f](https://github.com/OneLiteFeatherNET/Cygnus/commit/edb6f1f460b2f3197735c0db872a472d94995bb7))
* **cloudnet:** bind address from service properties + clean stdin shutdown ([#141](https://github.com/OneLiteFeatherNET/Cygnus/issues/141)) ([4abda36](https://github.com/OneLiteFeatherNET/Cygnus/commit/4abda36e4aba6bf78d7611e4346a8eb330d5870f))
* **cloudnet:** load the bridge as a Minestom extension in game and setup ([#148](https://github.com/OneLiteFeatherNET/Cygnus/issues/148)) ([07e2f8c](https://github.com/OneLiteFeatherNET/Cygnus/commit/07e2f8caf59b4b9ca9cb69b9239528ada275a55c))
* **data:** improve save logic ([d2b94ec](https://github.com/OneLiteFeatherNET/Cygnus/commit/d2b94ecb5f6a1d44223ca15eb1050521ef74a08a))
* **data:** improve survivor handling ([#169](https://github.com/OneLiteFeatherNET/Cygnus/issues/169)) ([843c964](https://github.com/OneLiteFeatherNET/Cygnus/commit/843c964fc3959526f25aebea61ab110acb4bc70e))
* **deps:** downgrade aonyx version from 0.6.1 to 0.6.0 ([8e6eef2](https://github.com/OneLiteFeatherNET/Cygnus/commit/8e6eef2de1b7686a62cc9cfcc95f3afe5f5ff503))
* **deps:** update aonyx to v0.7.3 ([#90](https://github.com/OneLiteFeatherNET/Cygnus/issues/90)) ([adf61af](https://github.com/OneLiteFeatherNET/Cygnus/commit/adf61af3539d48f2f180aaf2d39203e9b0d87949))
* **deps:** update aonyx to v0.8.0 ([#93](https://github.com/OneLiteFeatherNET/Cygnus/issues/93)) ([af2d6da](https://github.com/OneLiteFeatherNET/Cygnus/commit/af2d6da36a9da2c9e664eb05d245899f00849112))
* **deps:** update dependency com.google.guava:guava to v33.7.0-android ([#188](https://github.com/OneLiteFeatherNET/Cygnus/issues/188)) ([b10335e](https://github.com/OneLiteFeatherNET/Cygnus/commit/b10335e928ac4993360e2796ea0de6d481e9431e))
* **deps:** update dependency com.google.guava:guava to v33.7.0-jre ([#190](https://github.com/OneLiteFeatherNET/Cygnus/issues/190)) ([55662a0](https://github.com/OneLiteFeatherNET/Cygnus/commit/55662a0bfde83afcc571f9f36819b8ff8b3189e6))
* **deps:** update dependency com.google.guava:guava to v33.7.1-android ([#192](https://github.com/OneLiteFeatherNET/Cygnus/issues/192)) ([2a44715](https://github.com/OneLiteFeatherNET/Cygnus/commit/2a447151afddc270fc4e1c537bb18882acafefc7))
* **deps:** update dependency com.google.guava:guava to v33.7.1-jre ([#194](https://github.com/OneLiteFeatherNET/Cygnus/issues/194)) ([f62bcc4](https://github.com/OneLiteFeatherNET/Cygnus/commit/f62bcc49a840ed47d5871315a98dfce6970bee69))
* **deps:** update dependency com.google.protobuf:protobuf-java to v4.32.0 ([#4](https://github.com/OneLiteFeatherNET/Cygnus/issues/4)) ([4b92663](https://github.com/OneLiteFeatherNET/Cygnus/commit/4b92663af57d7e9dc600880c3c6d61b2a1d486a8))
* **deps:** update dependency com.google.protobuf:protobuf-java to v4.32.1 ([#12](https://github.com/OneLiteFeatherNET/Cygnus/issues/12)) ([2fd85f2](https://github.com/OneLiteFeatherNET/Cygnus/commit/2fd85f23f1d1a57e54d9887e42000ffa09a94090))
* **deps:** update dependency eu.cloudnetservice.cloudnet:bom to v4.0.0-rc16-snapshot ([79c60d0](https://github.com/OneLiteFeatherNET/Cygnus/commit/79c60d09324d107cc3ee65f267167bf35b3308a4))
* **deps:** update dependency eu.cloudnetservice.cloudnet:bom to v4.0.0-rc16-snapshot ([8f62acf](https://github.com/OneLiteFeatherNET/Cygnus/commit/8f62acf81723dcd8fbb8ffd3a47033d7034c3573))
* **deps:** update dependency eu.cloudnetservice.cloudnet:bom to v4.0.0-rc17-snapshot ([#37](https://github.com/OneLiteFeatherNET/Cygnus/issues/37)) ([1d0a7e6](https://github.com/OneLiteFeatherNET/Cygnus/commit/1d0a7e658b1717e214e00ecfec3aeff6287b2020))
* **deps:** update dependency net.onelitefeather:aonyx-bom to v0.7.0 ([cf8cbe8](https://github.com/OneLiteFeatherNET/Cygnus/commit/cf8cbe8b67f00980c1593ba159fe829f7aefc460))
* **deps:** update dependency net.onelitefeather:aonyx-bom to v0.7.0 ([e32c12b](https://github.com/OneLiteFeatherNET/Cygnus/commit/e32c12bc65c4b1cacf1ffa9615061db9073c7937))
* **deps:** update dependency net.onelitefeather:aonyx-bom to v0.7.1 ([#64](https://github.com/OneLiteFeatherNET/Cygnus/issues/64)) ([6752c6f](https://github.com/OneLiteFeatherNET/Cygnus/commit/6752c6fdf6dc4629f96d46c2a47bc68144120a34))
* **deps:** update dependency net.onelitefeather:falco-bom to v2 ([#158](https://github.com/OneLiteFeatherNET/Cygnus/issues/158)) ([1f40661](https://github.com/OneLiteFeatherNET/Cygnus/commit/1f406612543a35f91da60a63690c80253a7bfb54))
* **deps:** update dependency net.onelitefeather:mycelium-bom to v1.4.3 ([#7](https://github.com/OneLiteFeatherNET/Cygnus/issues/7)) ([7067cfc](https://github.com/OneLiteFeatherNET/Cygnus/commit/7067cfcff6bd9d6b795bfa6091a7760d1174d595))
* **deps:** update dependency net.onelitefeather:mycelium-bom to v1.4.4 ([#14](https://github.com/OneLiteFeatherNET/Cygnus/issues/14)) ([013f33b](https://github.com/OneLiteFeatherNET/Cygnus/commit/013f33b9141d04e3731940b0b7aeaf622e6f4f01))
* **deps:** update dependency net.onelitefeather:mycelium-bom to v1.5.3 ([#33](https://github.com/OneLiteFeatherNET/Cygnus/issues/33)) ([f80cc06](https://github.com/OneLiteFeatherNET/Cygnus/commit/f80cc0675185b2a4da7b4f6ecf3629e3f9f082e6))
* **deps:** update dependency net.onelitefeather:mycelium-bom to v1.6.0 ([b91f49f](https://github.com/OneLiteFeatherNET/Cygnus/commit/b91f49f955180b99fe16781eb3f8527e2221fb5a))
* **deps:** update dependency net.onelitefeather:mycelium-bom to v1.6.1 ([#39](https://github.com/OneLiteFeatherNET/Cygnus/issues/39)) ([dd025b9](https://github.com/OneLiteFeatherNET/Cygnus/commit/dd025b98b0e85a3b51b02bdd81257f73e2cc30bc))
* **deps:** update dependency net.onelitefeather:mycelium-bom to v1.6.2 ([#47](https://github.com/OneLiteFeatherNET/Cygnus/issues/47)) ([ad19315](https://github.com/OneLiteFeatherNET/Cygnus/commit/ad19315ab8c399b50902934efa4e34183327d314))
* **deps:** update dependency net.onelitefeather:mycelium-bom to v1.6.3 ([#53](https://github.com/OneLiteFeatherNET/Cygnus/issues/53)) ([354b22a](https://github.com/OneLiteFeatherNET/Cygnus/commit/354b22a9a0b1849627c875adae0760e853001639))
* **deps:** update dependency net.onelitefeather:mycelium-bom to v1.6.4 ([#59](https://github.com/OneLiteFeatherNET/Cygnus/issues/59)) ([d684d12](https://github.com/OneLiteFeatherNET/Cygnus/commit/d684d12ec9547f1d27b2571c505b6657c0084202))
* **deps:** update dependency net.onelitefeather:pica to v0.0.2 ([#85](https://github.com/OneLiteFeatherNET/Cygnus/issues/85)) ([beacf6f](https://github.com/OneLiteFeatherNET/Cygnus/commit/beacf6fbee3cb4e05a07f7e0e8ce5bb7082583eb))
* **deps:** update dependency net.onelitefeather:pica to v0.0.3 ([#87](https://github.com/OneLiteFeatherNET/Cygnus/issues/87)) ([ede3c50](https://github.com/OneLiteFeatherNET/Cygnus/commit/ede3c5069461f181b7e90c1492bb49b5ffe2f2fb))
* **deps:** update dependency net.onelitefeather:pica to v0.1.2 ([#174](https://github.com/OneLiteFeatherNET/Cygnus/issues/174)) ([f1e5fce](https://github.com/OneLiteFeatherNET/Cygnus/commit/f1e5fce2cc48fffa8eb65c226439b798f1aca974))
* **deps:** update dependency net.theevilreaper:aves to v1.11.2 ([#48](https://github.com/OneLiteFeatherNET/Cygnus/issues/48)) ([a8bc67c](https://github.com/OneLiteFeatherNET/Cygnus/commit/a8bc67c25d92cd40d6d6ed63ba4914e6d68b1063))
* **deps:** update dependency net.theevilreaper:aves to v1.13.0 ([7b41551](https://github.com/OneLiteFeatherNET/Cygnus/commit/7b41551c5ea55023fe13d609e0e2d2847f497501))
* **deps:** update dependency net.theevilreaper:aves to v1.13.0 ([#49](https://github.com/OneLiteFeatherNET/Cygnus/issues/49)) ([dddf946](https://github.com/OneLiteFeatherNET/Cygnus/commit/dddf946696f012ff65cdd0578df2ab0d43c34faa))
* **deps:** update dependency net.theevilreaper:aves to v1.13.1 ([#63](https://github.com/OneLiteFeatherNET/Cygnus/issues/63)) ([3933271](https://github.com/OneLiteFeatherNET/Cygnus/commit/3933271d20d5a1311b23d9f03b10731cce06974b))
* **deps:** update grpc-java monorepo to v1.68.3 ([#25](https://github.com/OneLiteFeatherNET/Cygnus/issues/25)) ([37cc573](https://github.com/OneLiteFeatherNET/Cygnus/commit/37cc573dd39d3b973d50da865d3b29f4685ae2d7))
* **deps:** update pica to v0.0.4 ([#92](https://github.com/OneLiteFeatherNET/Cygnus/issues/92)) ([b741b76](https://github.com/OneLiteFeatherNET/Cygnus/commit/b741b7690874bb2bacc3d072662df3b53bdb5d0d))
* **deps:** update pica to v0.1.0 ([#94](https://github.com/OneLiteFeatherNET/Cygnus/issues/94)) ([0ede5a7](https://github.com/OneLiteFeatherNET/Cygnus/commit/0ede5a7b622f830c547bc6d8aac73e821c81556e))
* **game:** add missing public keyword ([aba274e](https://github.com/OneLiteFeatherNET/Cygnus/commit/aba274ed76ce1ddb83483fe7d5aeeda69bcb0971))
* **game:** avoid double stamina service cleanup calls ([07417cd](https://github.com/OneLiteFeatherNET/Cygnus/commit/07417cdf7dfd09d0c9475e7732c4911452931f3d))
* **game:** restore old main class layout ([51edb9b](https://github.com/OneLiteFeatherNET/Cygnus/commit/51edb9bf738f3c2b414154ca135d37100023cef0))
* **helper:** avoid secure random usage and add missing documentation ([3deffe9](https://github.com/OneLiteFeatherNET/Cygnus/commit/3deffe95c5f7243abd18aa64ab65ad2fd55768c1))
* **helper:** improve random access value ([f12c806](https://github.com/OneLiteFeatherNET/Cygnus/commit/f12c8063c334a5064729d3db09039505015c70a9))
* improve player removal logic in TeamHelper ([f5cb8d6](https://github.com/OneLiteFeatherNET/Cygnus/commit/f5cb8d6456adb1f7bf278880e0c16aae79b3d6c6))
* **listener:** Remove undefined enum usage ([778a887](https://github.com/OneLiteFeatherNET/Cygnus/commit/778a8873f4fd6bf9bee7de3fd7a7455e1618291c))
* **lobby:** improve file check to avoid exception throw during the setup ([9a80218](https://github.com/OneLiteFeatherNET/Cygnus/commit/9a802181c5924dec86881ec1c9895b0ae70b8edf))
* **map:** accept the 26.2 world layout when filtering maps ([#151](https://github.com/OneLiteFeatherNET/Cygnus/issues/151)) ([499993e](https://github.com/OneLiteFeatherNET/Cygnus/commit/499993e209439f7745c2320071e00d989b4fb360))
* **map:** change instance unregister to avoid exception throw ([#161](https://github.com/OneLiteFeatherNET/Cygnus/issues/161)) ([80b274d](https://github.com/OneLiteFeatherNET/Cygnus/commit/80b274d568704ea1e616827f8035b7006c6a627e))
* **map:** improve null field serialization and deserialization ([#167](https://github.com/OneLiteFeatherNET/Cygnus/issues/167)) ([dcaac04](https://github.com/OneLiteFeatherNET/Cygnus/commit/dcaac0439854ee5fa41d9bb9049657065da3bff2))
* **message:** remove prefix usage from a component ([8978796](https://github.com/OneLiteFeatherNET/Cygnus/commit/897879680c39d02c06c2c9ad690d00c791f2ad9c))
* **meta:** add guard check for the ClientSettingsPacket ([#182](https://github.com/OneLiteFeatherNET/Cygnus/issues/182)) ([31d05cf](https://github.com/OneLiteFeatherNET/Cygnus/commit/31d05cfb25a0930948d29d81cc5b27a77851e12a))
* **page:** improve id determination ([#176](https://github.com/OneLiteFeatherNET/Cygnus/issues/176)) ([2ee7c8c](https://github.com/OneLiteFeatherNET/Cygnus/commit/2ee7c8cad357b894c058d123f6ece42a60a282d9))
* **page:** prevent adding duplicate pages ([#173](https://github.com/OneLiteFeatherNET/Cygnus/issues/173)) ([3ad9df7](https://github.com/OneLiteFeatherNET/Cygnus/commit/3ad9df7952797f2b7f80308c4500af90d9d4198e))
* **player:** keep chunks visible when a player changes instance ([#144](https://github.com/OneLiteFeatherNET/Cygnus/issues/144)) ([f5c11c7](https://github.com/OneLiteFeatherNET/Cygnus/commit/f5c11c73ae6eeade94c3809e9f8fe1d39b13afff))
* **player:** switch to a virtual border ([#185](https://github.com/OneLiteFeatherNET/Cygnus/issues/185)) ([c01f281](https://github.com/OneLiteFeatherNET/Cygnus/commit/c01f28160973fb34e17db5c16ec1b95f4b256b07))
* **quit:** improve revive logic ([1486306](https://github.com/OneLiteFeatherNET/Cygnus/commit/1486306219a95581877624380067a6c16178e2da))
* **release:** correct publish command in .releaserc.json ([b9cc2b5](https://github.com/OneLiteFeatherNET/Cygnus/commit/b9cc2b5031eabf83785b4b64a87cee3afb511eee))
* **release:** Replace semtaic releases ([#107](https://github.com/OneLiteFeatherNET/Cygnus/issues/107)) ([0f76ed7](https://github.com/OneLiteFeatherNET/Cygnus/commit/0f76ed7fbc43fb2d2d28ca3ea3f4d124d6a18126))
* **scare:** improve spawn logic and prevent spawning inside blocks ([#152](https://github.com/OneLiteFeatherNET/Cygnus/issues/152)) ([c46b5a5](https://github.com/OneLiteFeatherNET/Cygnus/commit/c46b5a54158f36ba3326d3a84fe131562c41ef39))
* **setup:** add missing instance set ([95a3544](https://github.com/OneLiteFeatherNET/Cygnus/commit/95a354421aa703e63f969d3cacc3726a12f2e3f0))
* stamina tests ([#76](https://github.com/OneLiteFeatherNET/Cygnus/issues/76)) ([1f13365](https://github.com/OneLiteFeatherNET/Cygnus/commit/1f13365df8bd31d0c78ab6acd56f09fb3a5fd03f))
* **stamina:** improve SlenderBar implementation ([#181](https://github.com/OneLiteFeatherNET/Cygnus/issues/181)) ([23d3449](https://github.com/OneLiteFeatherNET/Cygnus/commit/23d3449998695dad02a6d821b45809c09cf844bc))
* **stamina:** optimize stamina regeneration and consumption check ([#160](https://github.com/OneLiteFeatherNET/Cygnus/issues/160)) ([0313782](https://github.com/OneLiteFeatherNET/Cygnus/commit/0313782848b4522f8bf385942a86a431c2ade1b2))
* **team:** migrate team tests to the new structure ([62df296](https://github.com/OneLiteFeatherNET/Cygnus/commit/62df296d40073ef726d482c4889ecf9ba652e8ad))
* **team:** remove generic usage in parameter definition ([33def9e](https://github.com/OneLiteFeatherNET/Cygnus/commit/33def9efef49d04ff3e51b80fb0d0e2adc2c5521))
* **team:** update color import ([7029da9](https://github.com/OneLiteFeatherNET/Cygnus/commit/7029da9a053a77b8c0a39ae3d8f36050469bb219))
* **team:** use right name for the slender team creation ([ad18a42](https://github.com/OneLiteFeatherNET/Cygnus/commit/ad18a424641cbc10e7bd07eae2fc31e2fcd23189))
* **test:** disabled a test to refactor it later ([9acc3d9](https://github.com/OneLiteFeatherNET/Cygnus/commit/9acc3d9f135fcc774433be54f5b62370f861b390))
* **tests:** update PageResourceAdapterTest to use Vec instead of Pos ([3d01ba9](https://github.com/OneLiteFeatherNET/Cygnus/commit/3d01ba9998dad5f9d5e455e3b08442265282005d))
* **visibility:** enforce the role visibility matrix between all three roles ([#199](https://github.com/OneLiteFeatherNET/Cygnus/issues/199)) ([dcf8e8f](https://github.com/OneLiteFeatherNET/Cygnus/commit/dcf8e8f455346f0698735980ecd11056a27fb595))
* **workflow:** ensure SBOM upload runs only when version is available ([3f6f2b7](https://github.com/OneLiteFeatherNET/Cygnus/commit/3f6f2b7aeb3173ec5b31ca2d1f8166a14a492a48))
* **workflow:** update dependency for SBOM upload to depend on publish job instead of release-please ([aecdae9](https://github.com/OneLiteFeatherNET/Cygnus/commit/aecdae91f94c55ced196d03e203834d4837f0576))


### Miscellaneous Chores

* **main:** release 2.7.1 ([#203](https://github.com/OneLiteFeatherNET/Cygnus/issues/203)) ([ef147df](https://github.com/OneLiteFeatherNET/Cygnus/commit/ef147df831479f31948a9f065e3e81c1898a29b9))

## [2.7.1](https://github.com/OneLiteFeatherNET/Cygnus/compare/v2.7.0...v2.7.1) (2026-08-26)


### Bug Fixes

* **game:** add missing public keyword ([aba274e](https://github.com/OneLiteFeatherNET/Cygnus/commit/aba274ed76ce1ddb83483fe7d5aeeda69bcb0971))
* **game:** restore old main class layout ([51edb9b](https://github.com/OneLiteFeatherNET/Cygnus/commit/51edb9bf738f3c2b414154ca135d37100023cef0))

## [2.7.0](https://github.com/OneLiteFeatherNET/Cygnus/compare/v2.6.6...v2.7.0) (2026-08-26)


### Features

* add readme file ([5c3376e](https://github.com/OneLiteFeatherNET/Cygnus/commit/5c3376eb7fa830001e0f7f982a50e3bfd9089d50))
* **common:** add basic block handlers ([#197](https://github.com/OneLiteFeatherNET/Cygnus/issues/197)) ([33b3e48](https://github.com/OneLiteFeatherNET/Cygnus/commit/33b3e48108eeb4e8c94561ef157e14f0ebc491ee))
* **common:** add custom dimension presets ([#132](https://github.com/OneLiteFeatherNET/Cygnus/issues/132)) ([2e5482d](https://github.com/OneLiteFeatherNET/Cygnus/commit/2e5482dde4871dc491f09695976dca6d7a529429))
* **game:** add ability to use a custom page model ([#133](https://github.com/OneLiteFeatherNET/Cygnus/issues/133)) ([c069dbb](https://github.com/OneLiteFeatherNET/Cygnus/commit/c069dbb5bf73b9bd3eee2ef53721f11c2e9a3162))
* **game:** add mannequin entity ([#150](https://github.com/OneLiteFeatherNET/Cygnus/issues/150)) ([c86bffb](https://github.com/OneLiteFeatherNET/Cygnus/commit/c86bffb9a61ac17d8dc698777ae397416baec7dd))
* **game:** add player heartbeat ([#147](https://github.com/OneLiteFeatherNET/Cygnus/issues/147)) ([7c8f7f0](https://github.com/OneLiteFeatherNET/Cygnus/commit/7c8f7f056779896bcb0102c9f5ada94fa851e9e0))
* **game:** transition to night at the end of the lobby phase ([#165](https://github.com/OneLiteFeatherNET/Cygnus/issues/165)) ([8766880](https://github.com/OneLiteFeatherNET/Cygnus/commit/8766880f39b27eeac6be3b6f24c72e172abdcc12))
* **map:** add map announcement ([#171](https://github.com/OneLiteFeatherNET/Cygnus/issues/171)) ([3ed6b4d](https://github.com/OneLiteFeatherNET/Cygnus/commit/3ed6b4da05fd9f8d5fb19d78ad8cd275a6ab120d))
* **map:** read worlds through Falco instead of the Minestom loader ([#143](https://github.com/OneLiteFeatherNET/Cygnus/issues/143)) ([0c0ef46](https://github.com/OneLiteFeatherNET/Cygnus/commit/0c0ef465208db83838f1d3084caaa63a169388bb))
* **pack:** add resource pack handling ([#164](https://github.com/OneLiteFeatherNET/Cygnus/issues/164)) ([f1b1266](https://github.com/OneLiteFeatherNET/Cygnus/commit/f1b12665b3c70de9a16b0d84450967313d8005b3))
* **phase:** add sounds to the lobby countdown ([#166](https://github.com/OneLiteFeatherNET/Cygnus/issues/166)) ([a282883](https://github.com/OneLiteFeatherNET/Cygnus/commit/a282883bed65a5ffe918e6b9dc31cf141f22b78e))
* **repo:** add readme file ([#168](https://github.com/OneLiteFeatherNET/Cygnus/issues/168)) ([5c3376e](https://github.com/OneLiteFeatherNET/Cygnus/commit/5c3376eb7fa830001e0f7f982a50e3bfd9089d50))
* run Cygnus without LuckPerms ([#154](https://github.com/OneLiteFeatherNET/Cygnus/issues/154)) ([aa00aa3](https://github.com/OneLiteFeatherNET/Cygnus/commit/aa00aa3b78ec1a7e8d5216e6d4d35565fc646848))
* **setup:** add ability to delete locations in map setup ([#126](https://github.com/OneLiteFeatherNET/Cygnus/issues/126)) ([ea30cce](https://github.com/OneLiteFeatherNET/Cygnus/commit/ea30cced193ba1547d6d27e813b26bd1aa1ff0ac))
* **setup:** add default lore for fallback items ([#125](https://github.com/OneLiteFeatherNET/Cygnus/issues/125)) ([c2e324a](https://github.com/OneLiteFeatherNET/Cygnus/commit/c2e324a5d11e86e6eac5857e1a9af667d80321cd))
* **setup:** add page setup part ([#146](https://github.com/OneLiteFeatherNET/Cygnus/issues/146)) ([f035799](https://github.com/OneLiteFeatherNET/Cygnus/commit/f0357996f3ac51a78cf64fde77b1d2d27e9c706c))
* **setup:** improve setup item display ([#124](https://github.com/OneLiteFeatherNET/Cygnus/issues/124)) ([dd475df](https://github.com/OneLiteFeatherNET/Cygnus/commit/dd475df97e70afe79cf152c182fdc83bcc0374b9))
* **setup:** overhaul dialog data handling ([#128](https://github.com/OneLiteFeatherNET/Cygnus/issues/128)) ([622dcdf](https://github.com/OneLiteFeatherNET/Cygnus/commit/622dcdfd0e8b06afc94bb425dc77ab06578ffa39))
* **spectator:** add ability to spectate players ([#159](https://github.com/OneLiteFeatherNET/Cygnus/issues/159)) ([d29578b](https://github.com/OneLiteFeatherNET/Cygnus/commit/d29578b31bb55d6f662421bafa4449e06dd95376))
* **stats:** add simple ingame statistics ([#189](https://github.com/OneLiteFeatherNET/Cygnus/issues/189)) ([3c7b580](https://github.com/OneLiteFeatherNET/Cygnus/commit/3c7b580438ed39b515478ffd62074e0854d740b4))
* **teleport:** add strategy interface ([#135](https://github.com/OneLiteFeatherNET/Cygnus/issues/135)) ([bffa72f](https://github.com/OneLiteFeatherNET/Cygnus/commit/bffa72f0b4ea9992b4375b039af8eb3e7c49477f))


### Bug Fixes

* **ambient:** use survivor team ([75aff1a](https://github.com/OneLiteFeatherNET/Cygnus/commit/75aff1aaf42b6092456062be87b6080c91739bd3))
* **category:** avoid typo in one enum entry declaration ([b79d4e6](https://github.com/OneLiteFeatherNET/Cygnus/commit/b79d4e69b4e051732632eb9d85162494207eb67e))
* **cloudnet:** bind address from service properties + clean stdin shutdown ([#141](https://github.com/OneLiteFeatherNET/Cygnus/issues/141)) ([4abda36](https://github.com/OneLiteFeatherNET/Cygnus/commit/4abda36e4aba6bf78d7611e4346a8eb330d5870f))
* **cloudnet:** load the bridge as a Minestom extension in game and setup ([#148](https://github.com/OneLiteFeatherNET/Cygnus/issues/148)) ([07e2f8c](https://github.com/OneLiteFeatherNET/Cygnus/commit/07e2f8caf59b4b9ca9cb69b9239528ada275a55c))
* **data:** improve save logic ([d2b94ec](https://github.com/OneLiteFeatherNET/Cygnus/commit/d2b94ecb5f6a1d44223ca15eb1050521ef74a08a))
* **data:** improve survivor handling ([#169](https://github.com/OneLiteFeatherNET/Cygnus/issues/169)) ([843c964](https://github.com/OneLiteFeatherNET/Cygnus/commit/843c964fc3959526f25aebea61ab110acb4bc70e))
* **deps:** update dependency com.google.guava:guava to v33.7.0-android ([#188](https://github.com/OneLiteFeatherNET/Cygnus/issues/188)) ([b10335e](https://github.com/OneLiteFeatherNET/Cygnus/commit/b10335e928ac4993360e2796ea0de6d481e9431e))
* **deps:** update dependency com.google.guava:guava to v33.7.0-jre ([#190](https://github.com/OneLiteFeatherNET/Cygnus/issues/190)) ([55662a0](https://github.com/OneLiteFeatherNET/Cygnus/commit/55662a0bfde83afcc571f9f36819b8ff8b3189e6))
* **deps:** update dependency com.google.guava:guava to v33.7.1-android ([#192](https://github.com/OneLiteFeatherNET/Cygnus/issues/192)) ([2a44715](https://github.com/OneLiteFeatherNET/Cygnus/commit/2a447151afddc270fc4e1c537bb18882acafefc7))
* **deps:** update dependency com.google.guava:guava to v33.7.1-jre ([#194](https://github.com/OneLiteFeatherNET/Cygnus/issues/194)) ([f62bcc4](https://github.com/OneLiteFeatherNET/Cygnus/commit/f62bcc49a840ed47d5871315a98dfce6970bee69))
* **deps:** update dependency net.onelitefeather:falco-bom to v2 ([#158](https://github.com/OneLiteFeatherNET/Cygnus/issues/158)) ([1f40661](https://github.com/OneLiteFeatherNET/Cygnus/commit/1f406612543a35f91da60a63690c80253a7bfb54))
* **deps:** update dependency net.onelitefeather:pica to v0.1.2 ([#174](https://github.com/OneLiteFeatherNET/Cygnus/issues/174)) ([f1e5fce](https://github.com/OneLiteFeatherNET/Cygnus/commit/f1e5fce2cc48fffa8eb65c226439b798f1aca974))
* **game:** avoid double stamina service cleanup calls ([07417cd](https://github.com/OneLiteFeatherNET/Cygnus/commit/07417cdf7dfd09d0c9475e7732c4911452931f3d))
* **helper:** avoid secure random usage and add missing documentation ([3deffe9](https://github.com/OneLiteFeatherNET/Cygnus/commit/3deffe95c5f7243abd18aa64ab65ad2fd55768c1))
* **helper:** improve random access value ([f12c806](https://github.com/OneLiteFeatherNET/Cygnus/commit/f12c8063c334a5064729d3db09039505015c70a9))
* **lobby:** improve file check to avoid exception throw during the setup ([9a80218](https://github.com/OneLiteFeatherNET/Cygnus/commit/9a802181c5924dec86881ec1c9895b0ae70b8edf))
* **map:** accept the 26.2 world layout when filtering maps ([#151](https://github.com/OneLiteFeatherNET/Cygnus/issues/151)) ([499993e](https://github.com/OneLiteFeatherNET/Cygnus/commit/499993e209439f7745c2320071e00d989b4fb360))
* **map:** change instance unregister to avoid exception throw ([#161](https://github.com/OneLiteFeatherNET/Cygnus/issues/161)) ([80b274d](https://github.com/OneLiteFeatherNET/Cygnus/commit/80b274d568704ea1e616827f8035b7006c6a627e))
* **map:** improve null field serialization and deserialization ([#167](https://github.com/OneLiteFeatherNET/Cygnus/issues/167)) ([dcaac04](https://github.com/OneLiteFeatherNET/Cygnus/commit/dcaac0439854ee5fa41d9bb9049657065da3bff2))
* **message:** remove prefix usage from a component ([8978796](https://github.com/OneLiteFeatherNET/Cygnus/commit/897879680c39d02c06c2c9ad690d00c791f2ad9c))
* **meta:** add guard check for the ClientSettingsPacket ([#182](https://github.com/OneLiteFeatherNET/Cygnus/issues/182)) ([31d05cf](https://github.com/OneLiteFeatherNET/Cygnus/commit/31d05cfb25a0930948d29d81cc5b27a77851e12a))
* **page:** improve id determination ([#176](https://github.com/OneLiteFeatherNET/Cygnus/issues/176)) ([2ee7c8c](https://github.com/OneLiteFeatherNET/Cygnus/commit/2ee7c8cad357b894c058d123f6ece42a60a282d9))
* **page:** prevent adding duplicate pages ([#173](https://github.com/OneLiteFeatherNET/Cygnus/issues/173)) ([3ad9df7](https://github.com/OneLiteFeatherNET/Cygnus/commit/3ad9df7952797f2b7f80308c4500af90d9d4198e))
* **player:** keep chunks visible when a player changes instance ([#144](https://github.com/OneLiteFeatherNET/Cygnus/issues/144)) ([f5c11c7](https://github.com/OneLiteFeatherNET/Cygnus/commit/f5c11c73ae6eeade94c3809e9f8fe1d39b13afff))
* **player:** switch to a virtual border ([#185](https://github.com/OneLiteFeatherNET/Cygnus/issues/185)) ([c01f281](https://github.com/OneLiteFeatherNET/Cygnus/commit/c01f28160973fb34e17db5c16ec1b95f4b256b07))
* **quit:** improve revive logic ([1486306](https://github.com/OneLiteFeatherNET/Cygnus/commit/1486306219a95581877624380067a6c16178e2da))
* **scare:** improve spawn logic and prevent spawning inside blocks ([#152](https://github.com/OneLiteFeatherNET/Cygnus/issues/152)) ([c46b5a5](https://github.com/OneLiteFeatherNET/Cygnus/commit/c46b5a54158f36ba3326d3a84fe131562c41ef39))
* **stamina:** improve SlenderBar implementation ([#181](https://github.com/OneLiteFeatherNET/Cygnus/issues/181)) ([23d3449](https://github.com/OneLiteFeatherNET/Cygnus/commit/23d3449998695dad02a6d821b45809c09cf844bc))
* **stamina:** optimize stamina regeneration and consumption check ([#160](https://github.com/OneLiteFeatherNET/Cygnus/issues/160)) ([0313782](https://github.com/OneLiteFeatherNET/Cygnus/commit/0313782848b4522f8bf385942a86a431c2ade1b2))
* **team:** update color import ([7029da9](https://github.com/OneLiteFeatherNET/Cygnus/commit/7029da9a053a77b8c0a39ae3d8f36050469bb219))
* **team:** use right name for the slender team creation ([ad18a42](https://github.com/OneLiteFeatherNET/Cygnus/commit/ad18a424641cbc10e7bd07eae2fc31e2fcd23189))
* **visibility:** enforce the role visibility matrix between all three roles ([#199](https://github.com/OneLiteFeatherNET/Cygnus/issues/199)) ([dcf8e8f](https://github.com/OneLiteFeatherNET/Cygnus/commit/dcf8e8f455346f0698735980ecd11056a27fb595))

## [2.6.6](https://github.com/OneLiteFeatherNET/Cygnus/compare/v2.6.5...v2.6.6) (2026-06-29)


### Bug Fixes

* **workflow:** ensure SBOM upload runs only when version is available ([3f6f2b7](https://github.com/OneLiteFeatherNET/Cygnus/commit/3f6f2b7aeb3173ec5b31ca2d1f8166a14a492a48))

## [2.6.5](https://github.com/OneLiteFeatherNET/Cygnus/compare/v2.6.4...v2.6.5) (2026-06-29)


### Bug Fixes

* **ci:** trigger build ([fbdb09c](https://github.com/OneLiteFeatherNET/Cygnus/commit/fbdb09c2e233d993739820515ab8ad57ecc691fa))

## [2.6.4](https://github.com/OneLiteFeatherNET/Cygnus/compare/v2.6.3...v2.6.4) (2026-06-29)


### Bug Fixes

* **build:** simplify version extraction logic in build.gradle.kts ([#117](https://github.com/OneLiteFeatherNET/Cygnus/issues/117)) ([43b87f3](https://github.com/OneLiteFeatherNET/Cygnus/commit/43b87f3096a8d5bc667ce3d109255f340b85e806))

## [2.6.3](https://github.com/OneLiteFeatherNET/Cygnus/compare/v2.6.2...v2.6.3) (2026-06-29)


### Bug Fixes

* **ci:** trigger build ([edb6f1f](https://github.com/OneLiteFeatherNET/Cygnus/commit/edb6f1f460b2f3197735c0db872a472d94995bb7))

## [2.6.2](https://github.com/OneLiteFeatherNET/Cygnus/compare/v2.6.1...v2.6.2) (2026-06-29)


### Bug Fixes

* **workflow:** update dependency for SBOM upload to depend on publish job instead of release-please ([aecdae9](https://github.com/OneLiteFeatherNET/Cygnus/commit/aecdae91f94c55ced196d03e203834d4837f0576))

## [2.6.1](https://github.com/OneLiteFeatherNET/Cygnus/compare/v2.6.0...v2.6.1) (2026-06-29)


### Bug Fixes

* **release:** Replace semtaic releases ([#107](https://github.com/OneLiteFeatherNET/Cygnus/issues/107)) ([0f76ed7](https://github.com/OneLiteFeatherNET/Cygnus/commit/0f76ed7fbc43fb2d2d28ca3ea3f4d124d6a18126))

## [2.5.1](https://github.com/OneLiteFeatherNET/Cygnus/compare/v2.5.0...v2.5.1) (2026-06-28)


### Bug Fixes

* **setup:** add missing instance set ([95a3544](https://github.com/OneLiteFeatherNET/Cygnus/commit/95a354421aa703e63f969d3cacc3726a12f2e3f0))

# [2.5.0](https://github.com/OneLiteFeatherNET/Cygnus/compare/v2.4.0...v2.5.0) (2026-06-28)


### Features

* **setup:** add data overview inventory for the setup ([#102](https://github.com/OneLiteFeatherNET/Cygnus/issues/102)) ([c9b0e6a](https://github.com/OneLiteFeatherNET/Cygnus/commit/c9b0e6acec841ffd8c2a84a703bed12efca9826f))

# [2.4.0](https://github.com/OneLiteFeatherNET/Cygnus/compare/v2.3.8...v2.4.0) (2026-06-26)


### Features

* **setup:** add dialog related code ([#101](https://github.com/OneLiteFeatherNET/Cygnus/issues/101)) ([98b58bd](https://github.com/OneLiteFeatherNET/Cygnus/commit/98b58bdb79045c7792b4f6f2c39dafc4a09bb861))

## [2.3.8](https://github.com/OneLiteFeatherNET/Cygnus/compare/v2.3.7...v2.3.8) (2026-06-26)


### Bug Fixes

* **ambient:** improve service creation call ([147817e](https://github.com/OneLiteFeatherNET/Cygnus/commit/147817efd5cb8742c82da7d33ced1644414d90a8))

## [2.3.7](https://github.com/OneLiteFeatherNET/Cygnus/compare/v2.3.6...v2.3.7) (2026-06-15)


### Bug Fixes

* **deps:** update aonyx to v0.8.0 ([#93](https://github.com/OneLiteFeatherNET/Cygnus/issues/93)) ([af2d6da](https://github.com/OneLiteFeatherNET/Cygnus/commit/af2d6da36a9da2c9e664eb05d245899f00849112))
* **deps:** update pica to v0.1.0 ([#94](https://github.com/OneLiteFeatherNET/Cygnus/issues/94)) ([0ede5a7](https://github.com/OneLiteFeatherNET/Cygnus/commit/0ede5a7b622f830c547bc6d8aac73e821c81556e))

## [2.3.6](https://github.com/OneLiteFeatherNET/Cygnus/compare/v2.3.5...v2.3.6) (2026-06-12)


### Bug Fixes

* **release:** correct publish command in .releaserc.json ([b9cc2b5](https://github.com/OneLiteFeatherNET/Cygnus/commit/b9cc2b5031eabf83785b4b64a87cee3afb511eee))

## [2.3.5](https://github.com/OneLiteFeatherNET/Cygnus/compare/v2.3.4...v2.3.5) (2026-06-12)


### Bug Fixes

* **deps:** update pica to v0.0.4 ([#92](https://github.com/OneLiteFeatherNET/Cygnus/issues/92)) ([b741b76](https://github.com/OneLiteFeatherNET/Cygnus/commit/b741b7690874bb2bacc3d072662df3b53bdb5d0d))

## [2.3.4](https://github.com/OneLiteFeatherNET/Cygnus/compare/v2.3.3...v2.3.4) (2026-06-12)


### Bug Fixes

* **deps:** update aonyx to v0.7.3 ([#90](https://github.com/OneLiteFeatherNET/Cygnus/issues/90)) ([adf61af](https://github.com/OneLiteFeatherNET/Cygnus/commit/adf61af3539d48f2f180aaf2d39203e9b0d87949))

## [2.3.3](https://github.com/OneLiteFeatherNET/Cygnus/compare/v2.3.2...v2.3.3) (2026-05-13)


### Bug Fixes

* **deps:** update dependency net.onelitefeather:pica to v0.0.3 ([#87](https://github.com/OneLiteFeatherNET/Cygnus/issues/87)) ([ede3c50](https://github.com/OneLiteFeatherNET/Cygnus/commit/ede3c5069461f181b7e90c1492bb49b5ffe2f2fb))

## [2.3.2](https://github.com/OneLiteFeatherNET/Cygnus/compare/v2.3.1...v2.3.2) (2026-05-12)


### Bug Fixes

* **deps:** update dependency net.onelitefeather:pica to v0.0.2 ([#85](https://github.com/OneLiteFeatherNET/Cygnus/issues/85)) ([beacf6f](https://github.com/OneLiteFeatherNET/Cygnus/commit/beacf6fbee3cb4e05a07f7e0e8ce5bb7082583eb))

## [2.3.1](https://github.com/OneLiteFeatherNET/Cygnus/compare/v2.3.0...v2.3.1) (2026-05-01)


### Bug Fixes

* stamina tests ([#76](https://github.com/OneLiteFeatherNET/Cygnus/issues/76)) ([1f13365](https://github.com/OneLiteFeatherNET/Cygnus/commit/1f13365df8bd31d0c78ab6acd56f09fb3a5fd03f))

# [2.3.0](https://github.com/OneLiteFeatherNET/Cygnus/compare/v2.2.0...v2.3.0) (2026-04-25)


### Features

* introduce new setup data implementations ([#73](https://github.com/OneLiteFeatherNET/Cygnus/issues/73)) ([a4d7b9f](https://github.com/OneLiteFeatherNET/Cygnus/commit/a4d7b9fbd9675554678f48033cb87f04995cce37))

# [2.2.0](https://github.com/OneLiteFeatherNET/Cygnus/compare/v2.1.0...v2.2.0) (2026-04-25)


### Features

* **setup:** add setup player implementation ([#72](https://github.com/OneLiteFeatherNET/Cygnus/issues/72)) ([4f75571](https://github.com/OneLiteFeatherNET/Cygnus/commit/4f75571a61b6944fa4b532887998f48e97ad670a))
* **setup:** add setup player implementation ([#72](https://github.com/OneLiteFeatherNET/Cygnus/issues/72)) ([947cb5b](https://github.com/OneLiteFeatherNET/Cygnus/commit/947cb5bafa48144cbbfeb39de26521a9de2d11f6))

# [2.1.0](https://github.com/OneLiteFeatherNET/Cygnus/compare/v2.0.0...v2.1.0) (2026-04-24)


### Features

* add dialog base for the setup input ([#69](https://github.com/OneLiteFeatherNET/Cygnus/issues/69)) ([c8b2bfb](https://github.com/OneLiteFeatherNET/Cygnus/commit/c8b2bfbe8445f2bdd186acfa9d30b1bae0a596ba))

# [2.0.0](https://github.com/OneLiteFeatherNET/Cygnus/compare/v1.0.14...v2.0.0) (2026-04-20)

## [1.0.14](https://github.com/OneLiteFeatherNET/Cygnus/compare/v1.0.13...v1.0.14) (2026-04-09)


### Bug Fixes

* **deps:** update dependency net.theevilreaper:aves to v1.13.1 ([#63](https://github.com/OneLiteFeatherNET/Cygnus/issues/63)) ([3933271](https://github.com/OneLiteFeatherNET/Cygnus/commit/3933271d20d5a1311b23d9f03b10731cce06974b))

## [1.0.13](https://github.com/OneLiteFeatherNET/Cygnus/compare/v1.0.12...v1.0.13) (2026-04-08)


### Bug Fixes

* **deps:** update dependency net.onelitefeather:aonyx-bom to v0.7.1 ([#64](https://github.com/OneLiteFeatherNET/Cygnus/issues/64)) ([6752c6f](https://github.com/OneLiteFeatherNET/Cygnus/commit/6752c6fdf6dc4629f96d46c2a47bc68144120a34))

## [1.0.12](https://github.com/OneLiteFeatherNET/Cygnus/compare/v1.0.11...v1.0.12) (2026-04-02)


### Bug Fixes

* **deps:** update dependency net.onelitefeather:mycelium-bom to v1.6.4 ([#59](https://github.com/OneLiteFeatherNET/Cygnus/issues/59)) ([d684d12](https://github.com/OneLiteFeatherNET/Cygnus/commit/d684d12ec9547f1d27b2571c505b6657c0084202))

## [1.0.11](https://github.com/OneLiteFeatherNET/Cygnus/compare/v1.0.10...v1.0.11) (2026-03-08)


### Bug Fixes

* **deps:** update dependency net.onelitefeather:mycelium-bom to v1.6.3 ([#53](https://github.com/OneLiteFeatherNET/Cygnus/issues/53)) ([354b22a](https://github.com/OneLiteFeatherNET/Cygnus/commit/354b22a9a0b1849627c875adae0760e853001639))

## [1.0.10](https://github.com/OneLiteFeatherNET/Cygnus/compare/v1.0.9...v1.0.10) (2026-03-05)


### Bug Fixes

* **deps:** update dependency net.onelitefeather:mycelium-bom to v1.6.2 ([#47](https://github.com/OneLiteFeatherNET/Cygnus/issues/47)) ([ad19315](https://github.com/OneLiteFeatherNET/Cygnus/commit/ad19315ab8c399b50902934efa4e34183327d314))

## [1.0.9](https://github.com/OneLiteFeatherNET/Cygnus/compare/v1.0.8...v1.0.9) (2026-03-04)


### Bug Fixes

* **deps:** update dependency net.theevilreaper:aves to v1.13.0 ([7b41551](https://github.com/OneLiteFeatherNET/Cygnus/commit/7b41551c5ea55023fe13d609e0e2d2847f497501))
* **deps:** update dependency net.theevilreaper:aves to v1.13.0 ([#49](https://github.com/OneLiteFeatherNET/Cygnus/issues/49)) ([dddf946](https://github.com/OneLiteFeatherNET/Cygnus/commit/dddf946696f012ff65cdd0578df2ab0d43c34faa))

## [1.0.8](https://github.com/OneLiteFeatherNET/Cygnus/compare/v1.0.7...v1.0.8) (2026-03-03)


### Bug Fixes

* **deps:** update dependency net.theevilreaper:aves to v1.11.2 ([#48](https://github.com/OneLiteFeatherNET/Cygnus/issues/48)) ([a8bc67c](https://github.com/OneLiteFeatherNET/Cygnus/commit/a8bc67c25d92cd40d6d6ed63ba4914e6d68b1063))

## [1.0.7](https://github.com/OneLiteFeatherNET/Cygnus/compare/v1.0.6...v1.0.7) (2026-02-13)


### Bug Fixes

* **deps:** update dependency net.onelitefeather:aonyx-bom to v0.7.0 ([e32c12b](https://github.com/OneLiteFeatherNET/Cygnus/commit/e32c12bc65c4b1cacf1ffa9615061db9073c7937))

## [1.0.6](https://github.com/OneLiteFeatherNET/Cygnus/compare/v1.0.5...v1.0.6) (2026-02-04)


### Bug Fixes

* **deps:** update dependency net.onelitefeather:mycelium-bom to v1.6.1 ([#39](https://github.com/OneLiteFeatherNET/Cygnus/issues/39)) ([dd025b9](https://github.com/OneLiteFeatherNET/Cygnus/commit/dd025b98b0e85a3b51b02bdd81257f73e2cc30bc))

## [1.0.5](https://github.com/OneLiteFeatherNET/Cygnus/compare/v1.0.4...v1.0.5) (2026-02-04)


### Bug Fixes

* **deps:** update dependency eu.cloudnetservice.cloudnet:bom to v4.0.0-rc17-snapshot ([#37](https://github.com/OneLiteFeatherNET/Cygnus/issues/37)) ([1d0a7e6](https://github.com/OneLiteFeatherNET/Cygnus/commit/1d0a7e658b1717e214e00ecfec3aeff6287b2020))

## [1.0.4](https://github.com/OneLiteFeatherNET/Cygnus/compare/v1.0.3...v1.0.4) (2025-12-16)


### Bug Fixes

* **deps:** update dependency net.onelitefeather:mycelium-bom to v1.5.3 ([#33](https://github.com/OneLiteFeatherNET/Cygnus/issues/33)) ([f80cc06](https://github.com/OneLiteFeatherNET/Cygnus/commit/f80cc0675185b2a4da7b4f6ecf3629e3f9f082e6))

## [1.0.3](https://github.com/OneLiteFeatherNET/Cygnus/compare/v1.0.2...v1.0.3) (2025-11-27)


### Bug Fixes

* **deps:** update dependency eu.cloudnetservice.cloudnet:bom to v4.0.0-rc16-snapshot ([8f62acf](https://github.com/OneLiteFeatherNET/Cygnus/commit/8f62acf81723dcd8fbb8ffd3a47033d7034c3573))

## [1.0.2](https://github.com/OneLiteFeatherNET/Cygnus/compare/v1.0.1...v1.0.2) (2025-11-07)


### Bug Fixes

* **deps:** update grpc-java monorepo to v1.68.3 ([#25](https://github.com/OneLiteFeatherNET/Cygnus/issues/25)) ([37cc573](https://github.com/OneLiteFeatherNET/Cygnus/commit/37cc573dd39d3b973d50da865d3b29f4685ae2d7))

## [1.0.1](https://github.com/OneLiteFeatherNET/Cygnus/compare/v1.0.0...v1.0.1) (2025-11-06)


### Bug Fixes

* improve player removal logic in TeamHelper ([f5cb8d6](https://github.com/OneLiteFeatherNET/Cygnus/commit/f5cb8d6456adb1f7bf278880e0c16aae79b3d6c6))

# 1.0.0 (2025-11-05)


### Bug Fixes

* **attribute:** update movement attribute call ([84b66af](https://github.com/OneLiteFeatherNET/Cygnus/commit/84b66aff61061c06651cdefab6d407b05f666556))
* **chat:** fix component handling ([e1d6617](https://github.com/OneLiteFeatherNET/Cygnus/commit/e1d6617e9b618c49531b3e03a1d6d16bf2438c67))
* **deps:** downgrade aonyx version from 0.6.1 to 0.6.0 ([8e6eef2](https://github.com/OneLiteFeatherNET/Cygnus/commit/8e6eef2de1b7686a62cc9cfcc95f3afe5f5ff503))
* **deps:** update dependency com.google.protobuf:protobuf-java to v4.32.0 ([#4](https://github.com/OneLiteFeatherNET/Cygnus/issues/4)) ([4b92663](https://github.com/OneLiteFeatherNET/Cygnus/commit/4b92663af57d7e9dc600880c3c6d61b2a1d486a8))
* **deps:** update dependency com.google.protobuf:protobuf-java to v4.32.1 ([#12](https://github.com/OneLiteFeatherNET/Cygnus/issues/12)) ([2fd85f2](https://github.com/OneLiteFeatherNET/Cygnus/commit/2fd85f23f1d1a57e54d9887e42000ffa09a94090))
* **deps:** update dependency net.onelitefeather:mycelium-bom to v1.4.3 ([#7](https://github.com/OneLiteFeatherNET/Cygnus/issues/7)) ([7067cfc](https://github.com/OneLiteFeatherNET/Cygnus/commit/7067cfcff6bd9d6b795bfa6091a7760d1174d595))
* **deps:** update dependency net.onelitefeather:mycelium-bom to v1.4.4 ([#14](https://github.com/OneLiteFeatherNET/Cygnus/issues/14)) ([013f33b](https://github.com/OneLiteFeatherNET/Cygnus/commit/013f33b9141d04e3731940b0b7aeaf622e6f4f01))
* **listener:** Remove undefined enum usage ([778a887](https://github.com/OneLiteFeatherNET/Cygnus/commit/778a8873f4fd6bf9bee7de3fd7a7455e1618291c))
* **team:** migrate team tests to the new structure ([62df296](https://github.com/OneLiteFeatherNET/Cygnus/commit/62df296d40073ef726d482c4889ecf9ba652e8ad))
* **team:** remove generic usage in parameter definition ([33def9e](https://github.com/OneLiteFeatherNET/Cygnus/commit/33def9efef49d04ff3e51b80fb0d0e2adc2c5521))
* **test:** disabled a test to refactor it later ([9acc3d9](https://github.com/OneLiteFeatherNET/Cygnus/commit/9acc3d9f135fcc774433be54f5b62370f861b390))
* **tests:** update PageResourceAdapterTest to use Vec instead of Pos ([3d01ba9](https://github.com/OneLiteFeatherNET/Cygnus/commit/3d01ba9998dad5f9d5e455e3b08442265282005d))


### Features

* **build:** add CycloneDX plugin for dependency tracking ([cda6029](https://github.com/OneLiteFeatherNET/Cygnus/commit/cda6029fdf2991992e698220dd0a9187da3f0704))
* **build:** update publishing configuration and remove deprecated publishData plugin ([b79a07e](https://github.com/OneLiteFeatherNET/Cygnus/commit/b79a07eb688f0830393cddb12b6aee5f1b2b648a))
* **game:** Add game configuration, view interfaces, and Docker setup ([c65313e](https://github.com/OneLiteFeatherNET/Cygnus/commit/c65313e43fd9fb7c6c41a9d52ae81b64d6f7e5fa))
* **release:** implement semantic release workflow and update build configuration ([66b5f43](https://github.com/OneLiteFeatherNET/Cygnus/commit/66b5f4365eac5f4d3562cb9ee1c70d2ba7fe198e))

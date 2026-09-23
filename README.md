<p align="center">
   <img width="160" src="logo.png" alt="logo">
</p>

<h2 align="center">可怜BOT</h2>

<p align="center">支持舞萌DX/东方/活字印刷/表情包/音MAD功能的QQ机器人</p>

## 介绍

本项目基于[QQ开放平台](https://bot.q.qq.com/wiki/#%E7%AE%80%E4%BB%8B)提供的API开发，接入官方QQ机器人平台，提供音游、东方、语音、表情及音MAD相关的各机器人功能。

## 文档

[功能](https://bot-docs.otmdb.cn/features.html) [添加机器人](https://bot-docs.otmdb.cn/get-started.html)

## 项目结构

可怜BOT将各功能拆分为了不同插件，各功能插件均支持热重载。

```shell
KarenBot
├── src/        # 机器人本体功能
│
├── maimai/     # 舞萌DX
├── chunithm/   # 中二节奏
├── audio/      # 音频功能
├── meme/       # 表情包
├── random/     # 随机功能
├── text/       # 文本回复
├── admin/      # 管理功能
│
├── shinobu/    # 图片渲染模块
```

资源文件请移步至 [KarenBot-Resources](https://github.com/xszqxszq/KarenBot-Resources)。

## 问题反馈

Bug 或功能建议请在 [Issues](https://github.com/xszqxszq/KarenBot/issues) 中提出，也可提交 [Pull Request](https://github.com/xszqxszq/KarenBot/pulls)。

## 开发部署

本项目使用 Kotlin 进行开发，您可以阅读[文档](https://bot-docs.otmdb.cn/develop/deploy.html)来进行部署。

## 鸣谢

贡献者：[@algorithm1832](https://github.com/algorithm1832)

鸣谢：617、北京大的、soloopooo、浪涛I2A、唐辫小二、Qrsinko、MSC丶凌烟、慕湫风、MAYBOT、米若、山大/山威音游群、国科大音游群

致敬：[mirai](https://github.com/mamoe/mirai)，在第三方机器人时期可怜BOT主要基于mirai开发，目前的开发风格仍深受其影响

## 版权声明

本项目仅以学习交流为目的，所使用的资源均来自公开站点，请勿用于任何商业用途。

`maimai` / `chunithm` 模块所使用的 [maimai でらっくす](https://maimai.sega.jp/) 和 [CHUNITHM](https://chunithm.sega.jp) 相关的图片、音频素材版权均归 SEGA 所有，资源来源于公开站点，如 SEGA 官网、[diving-fish 查分器](https://maimai.diving-fish.com)、[落雪查分器](https://maimai.lxns.net)等。

`meme` 模块所使用的[プロジェクトセカイ](https://pjsekai.sega.jp)的图片素材来自 [TheOriginalAyaka/sekai-stickers](https://github.com/TheOriginalAyaka/sekai-stickers)，素材版权归 SEGA 所有；[Blue Archive](https://bluearchive.nexon.com/home) 风格 LOGO 的相关素材及生成代码逻辑来自 [nulla2011/bluearchive-logo](https://github.com/nulla2011/bluearchive-logo)，素材版权归 NEXON 所有；5000兆円欲しい风格图片的生成代码逻辑参考 [yurafuca/5000choyen](https://github.com/yurafuca/5000choyen)；Emoji 混合功能图片素材来自 [Emoji Kitchen](https://emojikitchen.dev)，版权归 Google 所有；其他表情包功能使用 [MemeCrafters/meme-generator-rs](https://github.com/MemeCrafters/meme-generator-rs) 进行生成。

`audio` 模块所使用的东方Project的音频和乐曲信息数据来自 [THBWiki](https://thbwiki.cc/原曲列表)，按 CC BY-NC-SA 3.0（署名-非商业-相同方式共享）使用，其版权归[上海アリス幻樂団](https://www16.big.or.jp/~zun/)所有。活字印刷功能使用的音频来自于 [WZQ02/HUOZI_web](https://github.com/WZQ02/HUOZI_web)。

`random` 模块所使用的金发表情包来源各不相同，版权归原版权方/原作者所有，本项目仅用于个人学习交流。

本项目所使用的开源字体包括：[思源黑体](https://github.com/adobe-fonts/source-han-sans)、[思源宋体](https://github.com/adobe-fonts/source-han-serif)、[未来荧黑](https://github.com/lvwzhen/glow-sans)；非开源字体包括：[阿里巴巴普惠体](https://fonts.alibabagroup.com/)、[方正兰亭系列](https://www.foundertype.com/)、[上首方糖体](http://www.ssfonts.com/shows/9/43.html)、[FOT-ユールカ Std](https://lets.fontworks.co.jp/fonts/218)。版权归字体作者所有，本项目仅出于学习交流目的而使用。

如您认为本项目侵犯到您的权益，请通过 [Issue](https://github.com/xszqxszq/KarenBot/issues) 提出，我们将会在第一时间进行处理。

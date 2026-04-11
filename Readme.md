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
├── otto/       # 活字印刷
├── meme/       # 表情包
├── guess/      # 东方猜原曲
├── text/       # 文本回复/随机表情
├── admin/      # 管理功能
│
├── shinobu/    # 图片渲染模块
│
└── data/       # 资源文件
```

## 问题反馈

Bug或功能建议请在[Issues](https://github.com/xszqxszq/KarenBot/issues)中提出，也可提交[Pull Request](https://github.com/xszqxszq/KarenBot/pulls)。

## 鸣谢

贡献者：[@algorithm1832](https://github.com/algorithm1832)

鸣谢：617、北京大的、浪涛I2A、唐辫小二、Qrsinko、MSC丶凌烟、慕湫风、MAYBOT、米若

致敬：[mirai](https://github.com/mamoe/mirai)，在第三方机器人时期可怜BOT主要基于mirai开发，目前的开发风格仍深受其影响
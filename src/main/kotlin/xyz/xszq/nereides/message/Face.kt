package xyz.xszq.nereides.message

class Face(val type: Int, val id: String): Message {
    enum class FaceType(val id: Int) {
        SYSTEM(1),
        EMOJI(2)
    }
    override fun contentToString(): String {
        if (type == FaceType.SYSTEM.id) {
            return "[face:$id:${names[id]?:""}]"
        }
        return "[face:$id]"
    }
    companion object {
        val names = buildMap {
            put("4", "得意")
            put("5", "流泪")
            put("8", "睡")
            put("9", "大哭")
            put("10", "尴尬")
            put("12", "调皮")
            put("14", "微笑")
            put("16", "酷")
            put("21", "可爱")
            put("23", "傲慢")
            put("24", "饥饿")
            put("25", "困")
            put("26", "惊恐")
            put("27", "流汗")
            put("28", "憨笑")
            put("29", "悠闲")
            put("30", "奋斗")
            put("32", "疑问")
            put("33", "嘘")
            put("34", "晕")
            put("38", "敲打")
            put("39", "再见")
            put("41", "发抖")
            put("42", "爱情")
            put("43", "跳跳")
            put("49", "拥抱")
            put("53", "蛋糕")
            put("60", "咖啡")
            put("63", "玫瑰")
            put("66", "爱心")
            put("74", "太阳")
            put("75", "月亮")
            put("76", "赞")
            put("78", "握手")
            put("79", "胜利")
            put("85", "飞吻")
            put("89", "西瓜")
            put("96", "冷汗")
            put("97", "擦汗")
            put("98", "抠鼻")
            put("99", "鼓掌")
            put("100", "糗大了")
            put("101", "坏笑")
            put("102", "左哼哼")
            put("103", "右哼哼")
            put("104", "哈欠")
            put("106", "委屈")
            put("109", "左亲亲")
            put("111", "可怜")
            put("116", "示爱")
            put("118", "抱拳")
            put("120", "拳头")
            put("122", "爱你")
            put("123", "NO")
            put("124", "OK")
            put("125", "转圈")
            put("129", "挥手")
            put("144", "喝彩")
            put("147", "棒棒糖")
            put("171", "茶")
            put("173", "泪奔")
            put("174", "无奈")
            put("175", "卖萌")
            put("176", "小纠结")
            put("179", "doge")
            put("180", "惊喜")
            put("181", "骚扰")
            put("182", "笑哭")
            put("183", "我最美")
            put("201", "点赞")
            put("203", "托脸")
            put("212", "托腮")
            put("214", "啵啵")
            put("219", "蹭一蹭")
            put("222", "抱抱")
            put("227", "拍手")
            put("232", "佛系")
            put("240", "喷脸")
            put("243", "甩头")
            put("246", "加油抱抱")
            put("262", "脑阔疼")
            put("264", "捂脸")
            put("265", "辣眼睛")
            put("266", "哦哟")
            put("267", "头秃")
            put("268", "问号脸")
            put("269", "暗中观察")
            put("270", "emm")
            put("271", "吃瓜")
            put("272", "呵呵哒")
            put("273", "我酸了")
            put("277", "汪汪")
            put("278", "汗")
            put("281", "无眼笑")
            put("282", "敬礼")
            put("284", "面无表情")
            put("285", "摸鱼")
            put("287", "哦")
            put("289", "睁眼")
            put("290", "敲开心")
            put("293", "摸锦鲤")
            put("294", "期待")
            put("297", "拜谢")
            put("298", "元宝")
            put("299", "牛啊")
            put("305", "右亲亲")
            put("306", "牛气冲天")
            put("307", "喵喵")
            put("314", "仔细分析")
            put("315", "加油")
            put("318", "崇拜")
            put("319", "比心")
            put("320", "庆祝")
            put("322", "拒绝")
            put("324", "吃糖")
            put("326", "生气")
        }
    }
}
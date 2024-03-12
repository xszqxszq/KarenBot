package xyz.xszq.bot.rhythmgame.lxns.payload

import kotlinx.serialization.Serializable

@Serializable
enum class RankType(val value: String) {
    SSSp("sssp"), SSS("sss"),
    SSp("ssp"), SS("ss"),
    Sp("sp"), S("s"),
    AAA("aaa"), AA("aa"), A("a"),
    BBB("bbb"), BB("bb"), B("b"),
    C("c"), D("d")
}

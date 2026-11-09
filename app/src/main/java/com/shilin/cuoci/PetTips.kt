package com.shilin.cuoci

import java.time.LocalDate

/**
 * 宠物时不时冒出来的一句提醒：高频词、熟词僻义、好词好句。
 */
object PetTips {

    val list: List<String> = listOf(
        "address 别只记「地址」，它还常考「处理、发表演讲」哦",
        "abandon 除了「放弃」，也有「放纵、放任」的意思",
        "appreciate 不只是「感激」，还常考「欣赏、理解」",
        "observe 除了「观察」，还有「遵守、评论」的意思",
        "issue 除了「问题」，也常考「发行、发布」",
        "company 除了「公司」，还有「陪伴」的意思呢",
        "figure 除了「数字」，还有「人物、认为」的用法",
        "capital 除了「首都」，还指「资本、大写字母」",
        "subject 除了「科目」，还能当动词「使遭受」",
        "写作里用 of vital importance 替掉 very important，会高级很多",
        "consequently 比 so 书面多了，作文结尾很好用",
        "Only in this way can we ... 这个倒装一用就加分",
        "The reason why ... is that ... 比 because 高级",
        "It is universally acknowledged that ... 是万能开头",
        "significant 比 important 更正式，注意别拼错",
        "contribute to 可以替换 help，还显得有逻辑",
        "not only ... but also 放句首要倒装，别忘了",
        "consider 比 think about 更书面，写作用它",
        "I would appreciate it if you could ... 礼貌又高级",
        "As is vividly shown in the chart ... 图表作文固定开头",
        "Taking all these factors into consideration 结尾很稳",
        "tackle 和 address 都能替掉 solve，别老用 solve",
        "难词不一定要背，会用 significant 这种才划算",
        "背单词别只看第一个意思，熟词僻义最容易丢分",
        "今天也背几个吧，我看着你呢"
    )

    /** 按日期轮换，一天一句 */
    fun todayTip(): String {
        val idx = LocalDate.now().dayOfYear % list.size
        return list[idx]
    }

    fun random(): String = list[(Math.random() * list.size).toInt().coerceIn(0, list.size - 1)]
}

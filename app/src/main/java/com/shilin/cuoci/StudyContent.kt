package com.shilin.cuoci

/**
 * 四六级写作干货（离线内置，可用 AI 换一批）。
 */
object StudyContent {

    data class Rep(
        val key: String,
        val keyCn: String,
        val vals: String,
        val valsCn: String
    )

    data class Pat(val tag: String, val en: String, val cn: String)

    val replacements: List<Rep> = listOf(
        Rep("very important", "非常重要", "of vital importance / crucial / indispensable", "至关重要的 / 不可或缺的"),
        Rep("good", "好的", "excellent / remarkable / outstanding", "出色的 / 卓越的 / 杰出的"),
        Rep("bad", "坏的", "detrimental / adverse / undesirable", "有害的 / 不利的 / 不可取的"),
        Rep("a lot of", "很多", "a considerable number of / an abundance of", "相当多的 / 大量的"),
        Rep("many people", "很多人", "a growing number of people / the majority of individuals", "越来越多的人 / 大多数人"),
        Rep("I think", "我认为", "from my perspective / as far as I am concerned", "在我看来 / 就我而言"),
        Rep("nowadays", "如今", "in contemporary society / at present", "在当代社会 / 目前"),
        Rep("because", "因为", "in that / owing to the fact that", "因为 / 由于……这一事实"),
        Rep("but", "但是", "nevertheless / whereas", "然而 / 而"),
        Rep("so", "所以", "therefore / consequently", "因此 / 于是"),
        Rep("get", "得到", "obtain / acquire / attain", "获得 / 习得 / 达到"),
        Rep("help", "帮助", "facilitate / be conducive to", "促进 / 有助于"),
        Rep("show", "表明", "demonstrate / reveal / illustrate", "证明 / 揭示 / 阐明"),
        Rep("use", "使用", "utilize / employ", "利用 / 运用"),
        Rep("problem", "问题", "dilemma / predicament / obstacle", "困境 / 窘境 / 障碍"),
        Rep("big", "大的", "considerable / substantial / immense", "相当大的 / 实质性的 / 巨大的"),
        Rep("more and more", "越来越多", "an increasing number of / a mounting number of", "日益增多的"),
        Rep("should", "应该", "be supposed to / it is advisable that", "应当 / 建议……"),
        Rep("interesting", "有趣的", "fascinating / thought-provoking", "引人入胜的 / 发人深省的"),
        Rep("famous", "著名的", "renowned / prestigious / celebrated", "享有盛誉的 / 有名望的"),
        Rep("difficult", "困难的", "challenging / formidable", "具有挑战性的 / 艰难巨大的"),
        Rep("improve", "改善", "enhance / optimize / strengthen", "提升 / 优化 / 加强"),
        Rep("cause", "导致", "give rise to / result in / trigger", "引起 / 导致 / 引发"),
        Rep("solve", "解决", "tackle / address / resolve", "应对 / 处理 / 解决"),
        Rep("know", "知道", "be aware of / be conscious of", "意识到 / 察觉到"),
        Rep("make sure", "确保", "ensure / guarantee", "确保 / 保证"),
        Rep("often", "经常", "frequently / on a regular basis", "频繁地 / 经常性地"),
        Rep("easy", "容易的", "accessible / effortless", "易于掌握的 / 毫不费力的")
    )

    val patterns: List<Pat> = listOf(
        Pat("开头", "It is universally acknowledged that ...", "人们普遍认为……"),
        Pat("开头", "With the rapid development of society, ... has become a heated topic.", "随着社会的迅速发展，……已成为热议话题。"),
        Pat("开头", "Opinions vary when it comes to ... Some argue that ..., while others hold the opposite view.", "关于……众说纷纭。有人认为……，也有人持相反观点。"),
        Pat("开头", "According to a recent survey, the number of ... has risen sharply over the past decade.", "据最近一项调查，……的数量在过去十年间急剧上升。"),
        Pat("主体", "There is no denying that ... What is more important, ...", "不可否认……更重要的是……"),
        Pat("主体", "What deserves special attention is that ...", "特别值得注意的是……"),
        Pat("主体", "A case in point is that ...", "一个恰当的例子是……"),
        Pat("主体", "This phenomenon can be largely attributed to ..., which in turn leads to ...", "这一现象主要归因于……，进而导致了……"),
        Pat("主体", "Admittedly, ... Nevertheless, the benefits far outweigh the drawbacks.", "诚然……然而利远大于弊。"),
        Pat("主体", "Compared with ..., ... enjoys obvious advantages in terms of ...", "与……相比，……在……方面优势明显。"),
        Pat("结尾", "Taking all these factors into consideration, we may safely draw the conclusion that ...", "综合考虑所有这些因素，我们完全可以得出结论……"),
        Pat("结尾", "It is high time that we took effective measures to ...", "是我们采取有效措施……的时候了。"),
        Pat("结尾", "Only in this way can we build a more harmonious society.", "只有这样，我们才能建设一个更加和谐的社会。"),
        Pat("结尾", "I am firmly convinced that ... will play an increasingly significant role in our lives.", "我坚信……将在我们的生活中发挥越来越重要的作用。"),
        Pat("图表", "As is vividly shown in the chart, the figure climbed steadily from ... to ...", "如图表清晰所示，该数字从……稳步上升至……"),
        Pat("图表", "The data reveals a marked decline in ... over the same period.", "数据显示同期……出现了明显下降。"),
        Pat("书信", "I am writing to express my sincere gratitude for ...", "我写信是为了对……表达诚挚的感谢。"),
        Pat("书信", "I would like to suggest that you ..., which I believe would be of great help.", "我建议你……，相信这会很有帮助。"),
        Pat("书信", "I am looking forward to your early reply.", "期待您的早日回复。"),
        Pat("强调", "It is ... that really matters, rather than ...", "真正重要的是……而不是……")
    )

    /** 可选的作文题材 */
    val topics: List<String> = listOf(
        "科技与人工智能", "教育公平", "环境保护", "社会热点",
        "文化交流", "个人成长", "经济发展", "健康生活"
    )
}

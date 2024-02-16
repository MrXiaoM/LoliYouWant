package top.mrxiaom.loliyouwant.utils

import java.util.regex.Pattern

// fork from
// https://github.com/red-vel/EnglishWordSingularizeAndPluralize/blob/3d856745958b1b724a8731c9a74f99e6a5024e39/src/main/java/EnglishWordUtil.java

// 正则模式和对应的替换字符串
internal class PatternAndReplacement(var pattern: Pattern, var replacement: String)

// 英文单词单数、复数形式转换
object EnglishWordUtil {
    //一、单复数同形
    private val uncountables: MutableList<String> = ArrayList()

    //二、单数 -> 复数的不规则变化
    private val unregularSingularToPlural: MutableList<PatternAndReplacement> = ArrayList()

    //三、单数 -> 复数
    private val singularToPlural: MutableList<PatternAndReplacement> = ArrayList()

    //四、复数 -> 单数的不规则变化
    private val unregularPluralToSingular: MutableList<PatternAndReplacement> = ArrayList()

    //五、复数 -> 单数
    private val pluralToSingular: MutableList<PatternAndReplacement> = ArrayList()

    init {
        //一、单复数同形
        uncountables.add("deer")
        uncountables.add("shoes")
        uncountables.add("sheep")
        uncountables.add("fish")
        uncountables.add("Chinese")
        uncountables.add("English")
        uncountables.add("Portuguese")
        uncountables.add("Lebanese")
        uncountables.add("Swiss")
        uncountables.add("Vietnamese")
        uncountables.add("Japanese")
        uncountables.add("economics")
        uncountables.add("news")
        uncountables.add("human")
        //二、单数 -> 复数的不规则变化
        unregularSingularToPlural.add(
            PatternAndReplacement(
                Pattern.compile("^person$", Pattern.CASE_INSENSITIVE),
                "people"
            )
        )
        unregularSingularToPlural.add(
            PatternAndReplacement(
                Pattern.compile("([^(Ger)])man", Pattern.CASE_INSENSITIVE),
                "$1men"
            )
        )
        unregularSingularToPlural.add(PatternAndReplacement(Pattern.compile("^man$", Pattern.CASE_INSENSITIVE), "men"))
        unregularSingularToPlural.add(
            PatternAndReplacement(
                Pattern.compile("^child$", Pattern.CASE_INSENSITIVE),
                "children"
            )
        )
        unregularSingularToPlural.add(
            PatternAndReplacement(
                Pattern.compile("^foot$", Pattern.CASE_INSENSITIVE),
                "feet"
            )
        )
        unregularSingularToPlural.add(
            PatternAndReplacement(
                Pattern.compile("^tooth$", Pattern.CASE_INSENSITIVE),
                "teeth"
            )
        )
        unregularSingularToPlural.add(
            PatternAndReplacement(
                Pattern.compile("^(m|l)ouse$", Pattern.CASE_INSENSITIVE),
                "$1ice"
            )
        )
        unregularSingularToPlural.add(
            PatternAndReplacement(
                Pattern.compile("^matrix$", Pattern.CASE_INSENSITIVE),
                "matrices"
            )
        )
        unregularSingularToPlural.add(
            PatternAndReplacement(
                Pattern.compile("^vertex$", Pattern.CASE_INSENSITIVE),
                "vertices"
            )
        )
        unregularSingularToPlural.add(PatternAndReplacement(Pattern.compile("^ox$", Pattern.CASE_INSENSITIVE), "oxen"))
        unregularSingularToPlural.add(
            PatternAndReplacement(
                Pattern.compile("^goose$", Pattern.CASE_INSENSITIVE),
                "geese"
            )
        )
        unregularSingularToPlural.add(
            PatternAndReplacement(
                Pattern.compile("^basis$", Pattern.CASE_INSENSITIVE),
                "bases"
            )
        )
        //三、单数 -> 复数
        //2、直接加"-s"的x国人
        singularToPlural.add(
            PatternAndReplacement(
                Pattern.compile(
                    "^((German)|(Russian)|(American)|(Italian)|(Indian)|(Canadian)|(Australian)|(Swede))$",
                    Pattern.CASE_INSENSITIVE
                ), "$1s"
            )
        )
        //3、以s, ss，x, ch, sh结尾的名词加"-es"
        singularToPlural.add(
            PatternAndReplacement(
                Pattern.compile("(s|ss|x|ch|sh)$", Pattern.CASE_INSENSITIVE),
                "$1es"
            )
        )
        //4、以元音字母+o结尾（除studio），后面加"-s"
        singularToPlural.add(PatternAndReplacement(Pattern.compile("^studio$", Pattern.CASE_INSENSITIVE), "studios"))
        singularToPlural.add(PatternAndReplacement(Pattern.compile("([aeiou])o$", Pattern.CASE_INSENSITIVE), "$1os"))
        //5、以辅音字母+o结尾（除studio,piano,kilo,photo)，后面加"-es"
        singularToPlural.add(
            PatternAndReplacement(
                Pattern.compile(
                    "^((pian)|(kil)|(phot))o$",
                    Pattern.CASE_INSENSITIVE
                ), "$1os"
            )
        )
        singularToPlural.add(PatternAndReplacement(Pattern.compile("([^aeiou])o$", Pattern.CASE_INSENSITIVE), "$1oes"))
        //6、以辅音字母加y结尾的名词，变y为i加"-es "
        singularToPlural.add(PatternAndReplacement(Pattern.compile("([^aeiou])y$", Pattern.CASE_INSENSITIVE), "$1ies"))
        //7、以元音字母加y结尾的名词直接加"-s"
        singularToPlural.add(PatternAndReplacement(Pattern.compile("([aeiou])y$", Pattern.CASE_INSENSITIVE), "$1ys"))
        //8、除了roof，gulf，proof，beef，staff，belief，cliff
        //以fe或f结尾的名词，把fe或f变为v加"-es"
        singularToPlural.add(
            PatternAndReplacement(
                Pattern.compile(
                    "^((roo)|(gul)|(proo)|(bee)|(staf)|(belie)|(clif))f$",
                    Pattern.CASE_INSENSITIVE
                ), "$1fs"
            )
        )
        singularToPlural.add(PatternAndReplacement(Pattern.compile("(fe|f)$", Pattern.CASE_INSENSITIVE), "ves"))
        //9、无连字号复合名词，后面名词变复数
        singularToPlural.add(PatternAndReplacement(Pattern.compile("(cake)$", Pattern.CASE_INSENSITIVE), "cakes"))
        singularToPlural.add(PatternAndReplacement(Pattern.compile("(watch)$", Pattern.CASE_INSENSITIVE), "watches"))
        singularToPlural.add(PatternAndReplacement(Pattern.compile("(chair)$", Pattern.CASE_INSENSITIVE), "chairs"))
        singularToPlural.add(PatternAndReplacement(Pattern.compile("(man)$", Pattern.CASE_INSENSITIVE), "men"))
        singularToPlural.add(PatternAndReplacement(Pattern.compile("(wife)$", Pattern.CASE_INSENSITIVE), "wives"))
        singularToPlural.add(PatternAndReplacement(Pattern.compile("(glass)$", Pattern.CASE_INSENSITIVE), "glasses"))
        singularToPlural.add(PatternAndReplacement(Pattern.compile("(house)$", Pattern.CASE_INSENSITIVE), "houses"))

        //四、复数 -> 单数的不规则变化
        unregularPluralToSingular.add(
            PatternAndReplacement(
                Pattern.compile("^people$", Pattern.CASE_INSENSITIVE),
                "person"
            )
        )
        unregularPluralToSingular.add(
            PatternAndReplacement(
                Pattern.compile("([^(Ger)])men", Pattern.CASE_INSENSITIVE),
                "$1man"
            )
        )
        unregularPluralToSingular.add(PatternAndReplacement(Pattern.compile("^men$", Pattern.CASE_INSENSITIVE), "man"))
        unregularPluralToSingular.add(
            PatternAndReplacement(
                Pattern.compile("^children$", Pattern.CASE_INSENSITIVE),
                "child"
            )
        )
        unregularPluralToSingular.add(
            PatternAndReplacement(
                Pattern.compile("^feet$", Pattern.CASE_INSENSITIVE),
                "foot"
            )
        )
        unregularPluralToSingular.add(
            PatternAndReplacement(
                Pattern.compile("^teeth$", Pattern.CASE_INSENSITIVE),
                "tooth"
            )
        )
        unregularPluralToSingular.add(
            PatternAndReplacement(
                Pattern.compile("^(m|l)ice$", Pattern.CASE_INSENSITIVE),
                "$1ouse"
            )
        )
        unregularPluralToSingular.add(
            PatternAndReplacement(
                Pattern.compile("^matrices$", Pattern.CASE_INSENSITIVE),
                "matrix"
            )
        )
        unregularPluralToSingular.add(
            PatternAndReplacement(
                Pattern.compile("^vertices$", Pattern.CASE_INSENSITIVE),
                "vertex"
            )
        )
        unregularPluralToSingular.add(PatternAndReplacement(Pattern.compile("^oxen$", Pattern.CASE_INSENSITIVE), "ox"))
        unregularPluralToSingular.add(
            PatternAndReplacement(
                Pattern.compile("^geese$", Pattern.CASE_INSENSITIVE),
                "goose"
            )
        )
        unregularPluralToSingular.add(
            PatternAndReplacement(
                Pattern.compile("^bases$", Pattern.CASE_INSENSITIVE),
                "basis"
            )
        )

        //五、复数 -> 单数
        //9、无连字号复合名词，后面名词变复数
        pluralToSingular.add(PatternAndReplacement(Pattern.compile("(cakes)$", Pattern.CASE_INSENSITIVE), "cake"))
        pluralToSingular.add(PatternAndReplacement(Pattern.compile("(watches)$", Pattern.CASE_INSENSITIVE), "watch"))
        pluralToSingular.add(PatternAndReplacement(Pattern.compile("(chairs)$", Pattern.CASE_INSENSITIVE), "chair"))
        pluralToSingular.add(PatternAndReplacement(Pattern.compile("(men)$", Pattern.CASE_INSENSITIVE), "man"))
        pluralToSingular.add(PatternAndReplacement(Pattern.compile("(wives)$", Pattern.CASE_INSENSITIVE), "wife"))
        pluralToSingular.add(PatternAndReplacement(Pattern.compile("(glasses)$", Pattern.CASE_INSENSITIVE), "glass"))
        pluralToSingular.add(PatternAndReplacement(Pattern.compile("(houses)$", Pattern.CASE_INSENSITIVE), "house"))
        //2、直接加"-s"的x国人
        pluralToSingular.add(
            PatternAndReplacement(
                Pattern.compile(
                    "^((German)|(Russian)|(American)|(Italian)|(Indian)|(Canadian)|(Australian)|(Swede))s$",
                    Pattern.CASE_INSENSITIVE
                ), "$1"
            )
        )
        //3、以s, ss，x, ch, sh结尾的名词加"-es"
        pluralToSingular.add(PatternAndReplacement(Pattern.compile("houses$", Pattern.CASE_INSENSITIVE), "house"))
        pluralToSingular.add(
            PatternAndReplacement(
                Pattern.compile("(s|ss|x|ch|sh)es$", Pattern.CASE_INSENSITIVE),
                "$1"
            )
        )
        //4、以元音字母+o结尾（除studio），后面加"-s"
        pluralToSingular.add(PatternAndReplacement(Pattern.compile("^studios", Pattern.CASE_INSENSITIVE), "studio"))
        pluralToSingular.add(PatternAndReplacement(Pattern.compile("^([aeiou])os$", Pattern.CASE_INSENSITIVE), "$1"))
        //5、以辅音字母+o结尾（除studio,piano,kilo,photo)，后面加"-es"
        pluralToSingular.add(
            PatternAndReplacement(
                Pattern.compile(
                    "^((pian)|(kil)|(phot))os$",
                    Pattern.CASE_INSENSITIVE
                ), "$1o"
            )
        )
        pluralToSingular.add(PatternAndReplacement(Pattern.compile("([^aeiou])oes$", Pattern.CASE_INSENSITIVE), "$1o"))
        //6、以辅音字母加y结尾的名词，变y为i加"-es "
        pluralToSingular.add(PatternAndReplacement(Pattern.compile("([^aeiou])ies$", Pattern.CASE_INSENSITIVE), "$1y"))
        //7、以元音字母加y结尾的名词直接加"-s"
        pluralToSingular.add(PatternAndReplacement(Pattern.compile("^([aeiou])ys$", Pattern.CASE_INSENSITIVE), "$1"))
        //8、除了roof，gulf，proof，beef，staff，belief，cliff
        //以fe或f结尾的名词，把fe或f变为v加"-es"
        pluralToSingular.add(
            PatternAndReplacement(
                Pattern.compile(
                    "^((roo)|(gul)|(proo)|(bee)|(staf)|(belie)|(clif))fs$",
                    Pattern.CASE_INSENSITIVE
                ), "$1f"
            )
        )
        pluralToSingular.add(
            PatternAndReplacement(
                Pattern.compile("^((kni)|(wi)|(li))ves$", Pattern.CASE_INSENSITIVE),
                "$1fe"
            )
        )
        pluralToSingular.add(PatternAndReplacement(Pattern.compile("ves$", Pattern.CASE_INSENSITIVE), "f"))
    }

    // 是否为单数形式
    fun isSingular(word: String): Boolean {
        if (word.isEmpty()) {
            return false
        }
        // 1、单复数同形
        if (uncountables.contains(word)) {
            return true
        }
        //2、不规则变化
        for (unregular in unregularSingularToPlural) {
            val matcher = unregular.pattern.matcher(word)
            if (matcher.find()) {
                return true
            }
        }
        //3、规则变化
        for (regular in singularToPlural) {
            val matcher = regular.pattern.matcher(word)
            if (matcher.find()) {
                return true
            }
        }
        return word[word.length - 1] != 's'
    }

    // 单数 -> 复数
    fun pluralize(word: String): String {
        if (word.isEmpty()) {
            return ""
        }
        //1、单复数同形
        if (uncountables.contains(word)) {
            return word
        }
        //2、不规则变化
        for (unregular in unregularSingularToPlural) {
            val matcher = unregular.pattern.matcher(word)
            if (matcher.find()) {
                return matcher.replaceAll(unregular.replacement)
            }
        }
        //3、规则变化
        for (regular in singularToPlural) {
            val matcher = regular.pattern.matcher(word)
            if (matcher.find()) {
                return matcher.replaceAll(regular.replacement)
            }
        }
        return word + "s"
    }

    // 是否为复数形式
    fun isPlural(word: String): Boolean {
        if (word.isEmpty()) {
            return false
        }
        // 1、单复数同形
        if (uncountables.contains(word)) {
            return true
        }
        //2、不规则变化
        for (unregular in unregularPluralToSingular) {
            val matcher = unregular.pattern.matcher(word)
            if (matcher.find()) {
                return true
            }
        }
        //3、规则变化
        for (regular in pluralToSingular) {
            val matcher = regular.pattern.matcher(word)
            if (matcher.find()) {
                return true
            }
        }
        return word[word.length - 1] == 's'
    }

    // 复数 -> 单数
    fun singularize(word: String): String {
        if (word.isEmpty()) {
            return ""
        }
        //1、单复数同形
        if (uncountables.contains(word)) {
            return word
        }
        //2、不规则变化
        for (unregular in unregularPluralToSingular) {
            val matcher = unregular.pattern.matcher(word)
            if (matcher.find()) {
                return matcher.replaceAll(unregular.replacement)
            }
        }
        //3、规则变化
        for (regular in pluralToSingular) {
            val matcher = regular.pattern.matcher(word)
            if (matcher.find()) {
                return matcher.replaceAll(regular.replacement)
            }
        }
        //去掉最后一个字母s
        return word.removeSuffix("s")
    }
}
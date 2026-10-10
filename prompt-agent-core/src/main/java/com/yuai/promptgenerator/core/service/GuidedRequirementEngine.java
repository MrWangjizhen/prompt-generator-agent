package com.yuai.promptgenerator.core.service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.yuai.promptgenerator.core.domain.RequirementProfile;
import com.yuai.promptgenerator.core.domain.TaskType;

public class GuidedRequirementEngine {

    public static final int MAX_DISCOVERY_TURNS = 12;

    private static final Map<String, String> QUESTIONS = questions();

    public NextStep next(RequirementProfile profile, TaskType taskType, int discoveryTurns) {
        List<String> missing = profile.missingFields(taskType);
        if (missing.isEmpty()) {
            return new NextStep(true, true, null, List.of());
        }
        if (discoveryTurns >= MAX_DISCOVERY_TURNS) {
            return new NextStep(true, true,
                    "还有部分信息未明确，我已将它们标记为待确认假设。请检查需求摘要后确认或继续补充。",
                    missing);
        }
        String field = missing.getFirst();
        return new NextStep(false, false, QUESTIONS.getOrDefault(field, "请补充“" + field + "”的具体要求。"), missing);
    }

    public record NextStep(boolean readyForConfirmation, boolean assumptionsRequired, String question,
                           List<String> missingFields) {
    }

    private static Map<String, String> questions() {
        Map<String, String> values = new LinkedHashMap<>();
        values.put("goal", "你最终希望模型完成什么任务？请描述一个可观察的结果。");
        values.put("context", "这项任务的背景是什么？有哪些模型必须知道的信息？");
        values.put("audience", "结果主要给谁使用？对方具备什么知识水平？");
        values.put("input", "实际使用时会提供哪些输入数据或材料？");
        values.put("outputFormat", "你希望输出采用什么结构、长度和格式？");
        values.put("constraints", "有哪些必须遵守的限制、禁区或资源边界？");
        values.put("successCriteria", "怎样判断输出是成功的？请给出验收标准。");
        values.put("tone", "希望文本呈现什么语气和风格？");
        values.put("language", "代码使用什么语言、框架及版本？");
        values.put("runtime", "代码将在哪个运行环境中执行？");
        values.put("acceptanceTests", "代码需要通过哪些测试或验收场景？");
        values.put("method", "分析应采用什么方法、框架或比较维度？");
        values.put("evidenceStandard", "分析结论需要什么证据标准和引用方式？");
        values.put("subject", "图像的主体是什么？需要突出哪些细节？");
        values.put("style", "希望采用什么视觉风格、媒介或参考方向？");
        values.put("composition", "画面构图、视角、光线和色彩有什么要求？");
        values.put("aspectRatio", "目标画幅比例和分辨率是什么？");
        values.put("role", "这个 Agent 的职责、专业身份和边界是什么？");
        values.put("allowedTools", "Agent 可以调用哪些工具？哪些操作需要确认？");
        values.put("terminationCondition", "Agent 在什么条件下应结束任务或请求人工介入？");
        values.put("safetyBoundaries", "Agent 必须遵守哪些安全、隐私和权限边界？");
        return Map.copyOf(values);
    }
}

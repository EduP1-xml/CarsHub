const { Octokit } = require("@octokit/rest");

const DEEPSEEK_API_KEY = process.env.DEEPSEEK_API_KEY;
const GH_TOKEN = process.env.GH_PROJECT_TOKEN;
const REPO_OWNER = process.env.GITHUB_REPOSITORY_OWNER;
const REPO_NAME = process.env.GITHUB_REPOSITORY.split("/")[1];
const ISSUE_TITLE = process.env.ISSUE_TITLE;
const ISSUE_BODY = process.env.ISSUE_BODY;

const octokit = new Octokit({ auth: GH_TOKEN });

async function run() {
    // Removidas as crases da string para evitar o SyntaxError
    const prompt = `Você é um Tech Lead especialista em Java/Spring Boot.
  Analise este requisito de sprint/funcionalidade:
  Título: ${ISSUE_TITLE}
  Descrição: ${ISSUE_BODY}

  Responda EXCLUSIVAMENTE com um array JSON puro (sem blocos de código markdown):
  [
    {
      "title": "Título resumido da task",
      "body": "Descrição detalhada do que o dev deve fazer (Spring Boot/Java)"
    }
  ]`;

    const response = await fetch("https://api.deepseek.com/chat/completions", {
        method: "POST",
        headers: {
            "Content-Type": "application/json",
            Authorization: `Bearer ${DEEPSEEK_API_KEY}`,
        },
        body: JSON.stringify({
            model: "deepseek-chat",
            messages: [{ role: "user", content: prompt }],
        }),
    });

    const data = await response.json();
    let rawContent = data.choices[0].message.content.trim();

    // Limpeza de segurança: remove crases residuais caso a IA as envie
    rawContent = rawContent.replace(/^```json/i, '').replace(/^```/i, '').replace(/```$/i, '').trim();

    const tasks = JSON.parse(rawContent);

    console.log(`DeepSeek gerou ${tasks.length} tarefas. Criando no GitHub...`);

    for (const task of tasks) {
        await octokit.rest.issues.create({
            owner: REPO_OWNER,
            repo: REPO_NAME,
            title: `[Task] ${task.title}`,
            body: `${task.body}\n\n---\n*Gerado automaticamente pelo DeepSeek*`,
            labels: ["enhancement", "deepseek-task"],
        });
        console.log(`✔ Criada task: ${task.title}`);
    }
}

run().catch(console.error);
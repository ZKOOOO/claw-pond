const TOKEN_KEY = "clawpond.jwt";

const state = {
    token: localStorage.getItem(TOKEN_KEY) || "",
    profile: null,
    openclaws: [],
    poolOpenClaws: [],
    workJobs: [],
    lobsters: [],
    editingId: null,
    selectedPoolOpenClawId: null
};

const authFeedback = document.getElementById("auth-feedback");
const resourceFeedback = document.getElementById("resource-feedback");
const poolFeedback = document.getElementById("pool-feedback");
const jobFeedback = document.getElementById("job-feedback");
const lobsterFeedback = document.getElementById("lobster-feedback");

const loginForm = document.getElementById("login-form");
const registerForm = document.getElementById("register-form");
const openClawForm = document.getElementById("openclaw-form");
const workJobForm = document.getElementById("work-job-form");
const lobsterForm = document.getElementById("lobster-form");

const openClawList = document.getElementById("openclaw-list");
const poolList = document.getElementById("pool-list");
const workJobList = document.getElementById("work-job-list");
const lobsterList = document.getElementById("lobster-list");

const loadProfileButton = document.getElementById("load-profile-btn");
const logoutButton = document.getElementById("logout-btn");
const loadOpenClawsButton = document.getElementById("load-openclaws-btn");
const loadPoolButton = document.getElementById("load-pool-btn");
const loadWorkJobsButton = document.getElementById("load-work-jobs-btn");
const loadLobstersButton = document.getElementById("load-lobsters-btn");
const authModeButtons = document.querySelectorAll("[data-auth-mode]");
const cancelEditButton = document.getElementById("cancel-edit-btn");
const resetInstanceButton = document.getElementById("reset-instance-btn");
const submitInstanceButton = document.getElementById("submit-instance-btn");
const formModeBadge = document.getElementById("form-mode-badge");
const activeInput = document.getElementById("active-input");
const poolTagFilter = document.getElementById("pool-tag-filter");
const selectedOpenClawChip = document.getElementById("selected-openclaw-chip");
const clearSelectedOpenClawButton = document.getElementById("clear-selected-openclaw-btn");

// 统一更新页面里的反馈提示。
function setFeedback(element, message, type = "") {
    element.textContent = message || "";
    element.className = "feedback-panel";
    if (type) {
        element.classList.add(type);
    }
}

// 切换登录和注册两个视图。
function switchAuthMode(mode) {
    authModeButtons.forEach((button) => {
        button.classList.toggle("active", button.dataset.authMode === mode);
    });
    loginForm.classList.toggle("hidden", mode !== "login");
    registerForm.classList.toggle("hidden", mode !== "register");
    setFeedback(authFeedback, "");
}

// 所有接口请求都走同一个入口，统一处理 JWT、JSON 和错误信息。
async function apiFetch(path, options = {}) {
    const isFormDataBody = options.body instanceof FormData;
    const headers = {
        ...(!isFormDataBody && options.body ? { "Content-Type": "application/json" } : {}),
        ...(options.headers || {})
    };

    if (state.token) {
        headers.Authorization = `Bearer ${state.token}`;
    }

    const response = await fetch(path, {
        ...options,
        headers
    });

    const text = await response.text();
    const data = text ? tryParseJson(text) : null;

    if (!response.ok) {
        throw new Error(extractErrorMessage(data) || `请求失败：${response.status}`);
    }

    return data;
}

function tryParseJson(text) {
    try {
        return JSON.parse(text);
    } catch (error) {
        return null;
    }
}

function extractErrorMessage(payload) {
    if (!payload) {
        return "";
    }
    if (payload.validationErrors) {
        return Object.entries(payload.validationErrors)
            .map(([field, message]) => `${field}：${message}`)
            .join(" | ");
    }
    return payload.message || payload.error || "";
}

function saveToken(token) {
    state.token = token || "";
    if (state.token) {
        localStorage.setItem(TOKEN_KEY, state.token);
    } else {
        localStorage.removeItem(TOKEN_KEY);
    }
}

// 把标签输入框内容切成数组，支持中英文逗号。
function splitTagText(value) {
    if (!value || !value.trim()) {
        return [];
    }
    return value
        .split(/[,，\n]/)
        .map((item) => item.trim())
        .filter(Boolean);
}

function formatDate(value) {
    if (!value) {
        return "-";
    }
    return new Date(value).toLocaleString("zh-CN", { hour12: false });
}

function escapeHtml(value) {
    return String(value ?? "")
        .replaceAll("&", "&amp;")
        .replaceAll("<", "&lt;")
        .replaceAll(">", "&gt;")
        .replaceAll("\"", "&quot;")
        .replaceAll("'", "&#39;");
}

function escapeAttribute(value) {
    return escapeHtml(value);
}

function renderTagChips(tagNames) {
    if (!tagNames || !tagNames.length) {
        return '<span class="tag-chip empty">未打标签</span>';
    }
    return tagNames.map((tag) => `<span class="tag-chip">${escapeHtml(tag)}</span>`).join("");
}

// 退出登录时一并清理本地会话、选中状态和编辑状态。
function clearSession() {
    saveToken("");
    state.profile = null;
    state.openclaws = [];
    state.poolOpenClaws = [];
    state.workJobs = [];
    state.lobsters = [];
    state.selectedPoolOpenClawId = null;
    workJobForm.reset();
    lobsterForm.reset();
    renderProfile();
    renderSummary();
    renderManagedOpenClaws();
    renderPoolOpenClaws();
    renderWorkJobs();
    renderLobsters();
    resetInstanceForm();
    renderSelectedOpenClaw();
    setFeedback(authFeedback, "已退出登录。", "success");
    setFeedback(resourceFeedback, "");
    setFeedback(poolFeedback, "");
    setFeedback(jobFeedback, "");
    setFeedback(lobsterFeedback, "");
}

function renderProfile() {
    const empty = document.getElementById("session-empty");
    const profile = document.getElementById("session-profile");

    if (!state.profile) {
        empty.classList.remove("hidden");
        profile.classList.add("hidden");
        return;
    }

    document.getElementById("profile-username").textContent = state.profile.username;
    document.getElementById("profile-email").textContent = state.profile.email;
    document.getElementById("profile-role").textContent = state.profile.role;
    document.getElementById("profile-created").textContent = formatDate(state.profile.createdAt);

    empty.classList.add("hidden");
    profile.classList.remove("hidden");
}

// 根据自己的 OpenClaw 列表实时刷新概览卡片。
function renderSummary() {
    const total = state.openclaws.length;
    const active = state.openclaws.filter((item) => item.active).length;
    const latest = state.openclaws[0] || null;

    document.getElementById("summary-total").textContent = String(total);
    document.getElementById("summary-active").textContent = String(active);
    document.getElementById("summary-latest-name").textContent = latest ? latest.name : "暂无记录";
    document.getElementById("summary-latest-time").textContent = latest
        ? `接入时间：${formatDate(latest.createdAt)}`
        : "还没有接入任何实例";
}

// 渲染我管理的 OpenClaw 列表，支持编辑和删除。
function renderManagedOpenClaws() {
    const empty = document.getElementById("inventory-empty");

    if (!state.openclaws.length) {
        empty.classList.remove("hidden");
        openClawList.classList.add("hidden");
        openClawList.innerHTML = "";
        return;
    }

    openClawList.innerHTML = state.openclaws.map((item) => `
        <article class="instance-card">
            <div class="instance-head">
                <strong>${escapeHtml(item.name)}</strong>
                <span class="status-pill ${item.active ? "" : "inactive"}">${item.active ? "启用中" : "已停用"}</span>
            </div>
            <a class="instance-url" href="${escapeAttribute(item.baseUrl)}" target="_blank" rel="noreferrer">${escapeHtml(item.baseUrl)}</a>
            <p>${escapeHtml(item.description || "暂无描述")}</p>
            <div class="tag-cluster">${renderTagChips(item.tagNames)}</div>
            <div class="instance-meta">
                <span>外部标识：${escapeHtml(item.externalId)}</span>
                <span>归属用户：${escapeHtml(item.ownerUsername)}</span>
                <span>创建时间：${escapeHtml(formatDate(item.createdAt))}</span>
            </div>
            <div class="instance-actions">
                <button class="ghost-btn" type="button" data-action="edit" data-id="${escapeAttribute(item.id)}">编辑</button>
                <button class="ghost-btn danger-btn" type="button" data-action="delete" data-id="${escapeAttribute(item.id)}">删除</button>
            </div>
        </article>
    `).join("");

    empty.classList.add("hidden");
    openClawList.classList.remove("hidden");
}

// 渲染共享 OpenClaw 池，支持按标签筛选和选择实例。
function renderPoolOpenClaws() {
    const empty = document.getElementById("pool-empty");

    if (!state.poolOpenClaws.length) {
        empty.classList.remove("hidden");
        poolList.classList.add("hidden");
        poolList.innerHTML = "";
        return;
    }

    poolList.innerHTML = state.poolOpenClaws.map((item) => {
        const selected = item.id === state.selectedPoolOpenClawId;
        return `
            <article class="instance-card ${selected ? "selected-card" : ""}">
                <div class="instance-head">
                    <strong>${escapeHtml(item.name)}</strong>
                    <span class="status-pill">${selected ? "已选中" : "可用"}</span>
                </div>
                <a class="instance-url" href="${escapeAttribute(item.baseUrl)}" target="_blank" rel="noreferrer">${escapeHtml(item.baseUrl)}</a>
                <p>${escapeHtml(item.description || "暂无描述")}</p>
                <div class="tag-cluster">${renderTagChips(item.tagNames)}</div>
                <div class="instance-meta">
                    <span>外部标识：${escapeHtml(item.externalId)}</span>
                    <span>归属用户：${escapeHtml(item.ownerUsername)}</span>
                </div>
                <div class="instance-actions">
                    <button class="ghost-btn ${selected ? "primary-like" : ""}" type="button" data-pool-action="select" data-id="${escapeAttribute(item.id)}">
                        ${selected ? "当前已选" : "选它干活"}
                    </button>
                </div>
            </article>
        `;
    }).join("");

    empty.classList.add("hidden");
    poolList.classList.remove("hidden");
}

// 渲染任务单列表。
function renderWorkJobs() {
    const empty = document.getElementById("work-job-empty");

    if (!state.workJobs.length) {
        empty.classList.remove("hidden");
        workJobList.classList.add("hidden");
        workJobList.innerHTML = "";
        return;
    }

    workJobList.innerHTML = state.workJobs.map((job) => `
        <article class="job-card">
            <div class="instance-head">
                <strong>${escapeHtml(job.title)}</strong>
                <span class="status-pill">${escapeHtml(job.status)}</span>
            </div>
            <p>${escapeHtml(job.description || "暂无任务说明")}</p>
            <div class="tag-cluster">${renderTagChips(job.desiredTags)}</div>
            <div class="instance-meta">
                <span>执行 OpenClaw：${escapeHtml(job.openClawName)}</span>
                <span>OpenClaw 标签：${escapeHtml((job.openClawTags || []).join(", ") || "未打标签")}</span>
                <span>创建时间：${escapeHtml(formatDate(job.createdAt))}</span>
            </div>
        </article>
    `).join("");

    empty.classList.add("hidden");
    workJobList.classList.remove("hidden");
}

// 渲染龙虾资产列表。
function renderLobsters() {
    const empty = document.getElementById("lobster-empty");

    if (!state.lobsters.length) {
        empty.classList.remove("hidden");
        lobsterList.classList.add("hidden");
        lobsterList.innerHTML = "";
        return;
    }

    lobsterList.innerHTML = state.lobsters.map((lobster) => `
        <article class="asset-card">
            <div class="instance-head">
                <strong>${escapeHtml(lobster.name)}</strong>
                <a class="text-link" href="${escapeAttribute(lobster.downloadUrl)}">下载文件</a>
            </div>
            <p>${escapeHtml(lobster.description || "暂无说明")}</p>
            <div class="tag-cluster">${renderTagChips(lobster.tagNames)}</div>
            <div class="instance-meta">
                <span>文件名：${escapeHtml(lobster.originalFilename)}</span>
                <span>上传时间：${escapeHtml(formatDate(lobster.createdAt))}</span>
            </div>
        </article>
    `).join("");

    empty.classList.add("hidden");
    lobsterList.classList.remove("hidden");
}

function renderSelectedOpenClaw() {
    const selected = state.poolOpenClaws.find((item) => item.id === state.selectedPoolOpenClawId) || null;
    if (!selected) {
        selectedOpenClawChip.textContent = "尚未选择 OpenClaw";
        selectedOpenClawChip.classList.remove("editing");
        clearSelectedOpenClawButton.classList.add("hidden");
        return;
    }

    selectedOpenClawChip.textContent = `已选择：${selected.name}`;
    selectedOpenClawChip.classList.add("editing");
    clearSelectedOpenClawButton.classList.remove("hidden");
}

// 进入编辑模式时，把实例表单填充为当前数据。
function startEdit(id) {
    const item = state.openclaws.find((entry) => entry.id === id);
    if (!item) {
        setFeedback(resourceFeedback, "没有找到要编辑的实例。", "error");
        return;
    }

    state.editingId = item.id;
    openClawForm.elements.name.value = item.name;
    openClawForm.elements.baseUrl.value = item.baseUrl;
    openClawForm.elements.externalId.value = item.externalId;
    openClawForm.elements.description.value = item.description || "";
    openClawForm.elements.apiToken.value = "";
    openClawForm.elements.tagText.value = (item.tagNames || []).join(", ");
    activeInput.checked = item.active;

    formModeBadge.textContent = "编辑模式";
    formModeBadge.classList.add("editing");
    submitInstanceButton.textContent = "保存修改";
    cancelEditButton.classList.remove("hidden");
    setFeedback(resourceFeedback, `已进入编辑模式：${item.name}`, "success");
    openClawForm.scrollIntoView({ behavior: "smooth", block: "start" });
}

// 重置实例表单，并退出编辑模式。
function resetInstanceForm() {
    state.editingId = null;
    openClawForm.reset();
    activeInput.checked = true;
    formModeBadge.textContent = "创建模式";
    formModeBadge.classList.remove("editing");
    submitInstanceButton.textContent = "创建实例";
    cancelEditButton.classList.add("hidden");
}

function clearSelectedOpenClaw() {
    state.selectedPoolOpenClawId = null;
    renderSelectedOpenClaw();
    renderPoolOpenClaws();
}

function selectPoolOpenClaw(id) {
    state.selectedPoolOpenClawId = id;
    renderSelectedOpenClaw();
    renderPoolOpenClaws();
    setFeedback(jobFeedback, "已选择 OpenClaw，可以开始创建任务单。", "success");
}

async function loadProfile(showFeedback = true) {
    if (!state.token) {
        state.profile = null;
        renderProfile();
        return;
    }

    try {
        state.profile = await apiFetch("/api/auth/me", { method: "GET" });
        renderProfile();
        if (showFeedback) {
            setFeedback(authFeedback, "当前身份已刷新。", "success");
        }
    } catch (error) {
        clearSession();
        setFeedback(authFeedback, `会话已失效：${error.message}`, "error");
    }
}

async function loadManagedOpenClaws(showFeedback = false) {
    if (!state.token) {
        state.openclaws = [];
        renderSummary();
        renderManagedOpenClaws();
        return;
    }

    try {
        state.openclaws = await apiFetch("/api/openclaws", { method: "GET" });
        renderSummary();
        renderManagedOpenClaws();

        if (state.editingId && !state.openclaws.some((item) => item.id === state.editingId)) {
            resetInstanceForm();
        }

        if (showFeedback) {
            setFeedback(resourceFeedback, `已加载 ${state.openclaws.length} 个实例。`, "success");
        }
    } catch (error) {
        setFeedback(resourceFeedback, `加载实例失败：${error.message}`, "error");
    }
}

async function loadPoolOpenClaws(showFeedback = false) {
    if (!state.token) {
        state.poolOpenClaws = [];
        renderSelectedOpenClaw();
        renderPoolOpenClaws();
        return;
    }

    const tags = splitTagText(poolTagFilter.value);
    const query = new URLSearchParams();
    tags.forEach((tag) => query.append("tag", tag));
    const path = query.toString() ? `/api/openclaw-pool?${query}` : "/api/openclaw-pool";

    try {
        state.poolOpenClaws = await apiFetch(path, { method: "GET" });
        if (state.selectedPoolOpenClawId && !state.poolOpenClaws.some((item) => item.id === state.selectedPoolOpenClawId)) {
            state.selectedPoolOpenClawId = null;
        }
        renderSelectedOpenClaw();
        renderPoolOpenClaws();
        if (showFeedback) {
            setFeedback(poolFeedback, `已加载 ${state.poolOpenClaws.length} 个资源池实例。`, "success");
        }
    } catch (error) {
        setFeedback(poolFeedback, `加载资源池失败：${error.message}`, "error");
    }
}

async function loadWorkJobs(showFeedback = false) {
    if (!state.token) {
        state.workJobs = [];
        renderWorkJobs();
        return;
    }

    try {
        state.workJobs = await apiFetch("/api/work-jobs", { method: "GET" });
        renderWorkJobs();
        if (showFeedback) {
            setFeedback(jobFeedback, `已加载 ${state.workJobs.length} 条任务单。`, "success");
        }
    } catch (error) {
        setFeedback(jobFeedback, `加载任务单失败：${error.message}`, "error");
    }
}

async function loadLobsters(showFeedback = false) {
    if (!state.token) {
        state.lobsters = [];
        renderLobsters();
        return;
    }

    try {
        state.lobsters = await apiFetch("/api/lobsters", { method: "GET" });
        renderLobsters();
        if (showFeedback) {
            setFeedback(lobsterFeedback, `已加载 ${state.lobsters.length} 只龙虾。`, "success");
        }
    } catch (error) {
        setFeedback(lobsterFeedback, `加载龙虾失败：${error.message}`, "error");
    }
}

async function loadAllAuthenticatedData() {
    await loadProfile(false);
    await loadManagedOpenClaws(false);
    await loadPoolOpenClaws(false);
    await loadWorkJobs(false);
    await loadLobsters(false);
}

// 创建和更新共用同一张实例表单，通过 editingId 判断当前模式。
async function submitInstanceForm(event) {
    event.preventDefault();
    if (!state.token) {
        setFeedback(resourceFeedback, "请先登录，再进行实例管理。", "error");
        return;
    }

    const isEditing = Boolean(state.editingId);
    const payload = {
        name: openClawForm.elements.name.value,
        baseUrl: openClawForm.elements.baseUrl.value,
        externalId: openClawForm.elements.externalId.value,
        description: openClawForm.elements.description.value,
        apiToken: openClawForm.elements.apiToken.value,
        tagNames: splitTagText(openClawForm.elements.tagText.value)
    };

    if (isEditing) {
        payload.active = activeInput.checked;
    }

    try {
        const result = await apiFetch(
            isEditing ? `/api/openclaws/${state.editingId}` : "/api/openclaws",
            {
                method: isEditing ? "PUT" : "POST",
                body: JSON.stringify(payload)
            }
        );

        setFeedback(
            resourceFeedback,
            isEditing ? `实例“${result.name}”已更新。` : `实例“${result.name}”已创建。`,
            "success"
        );
        resetInstanceForm();
        await loadManagedOpenClaws(false);
        await loadPoolOpenClaws(false);
    } catch (error) {
        setFeedback(
            resourceFeedback,
            isEditing ? `更新失败：${error.message}` : `创建失败：${error.message}`,
            "error"
        );
    }
}

async function deleteManagedOpenClaw(id) {
    const item = state.openclaws.find((entry) => entry.id === id);
    if (!item) {
        setFeedback(resourceFeedback, "没有找到要删除的实例。", "error");
        return;
    }

    const confirmed = window.confirm(`确认删除实例“${item.name}”吗？此操作不可撤销。`);
    if (!confirmed) {
        return;
    }

    try {
        await apiFetch(`/api/openclaws/${id}`, { method: "DELETE" });
        if (state.editingId === id) {
            resetInstanceForm();
        }
        if (state.selectedPoolOpenClawId === id) {
            clearSelectedOpenClaw();
        }
        setFeedback(resourceFeedback, `实例“${item.name}”已删除。`, "success");
        await loadManagedOpenClaws(false);
        await loadPoolOpenClaws(false);
    } catch (error) {
        setFeedback(resourceFeedback, `删除失败：${error.message}`, "error");
    }
}

async function submitWorkJob(event) {
    event.preventDefault();
    if (!state.token) {
        setFeedback(jobFeedback, "请先登录，再创建任务单。", "error");
        return;
    }
    if (!state.selectedPoolOpenClawId) {
        setFeedback(jobFeedback, "请先从 OpenClaw 池中选择一个实例。", "error");
        return;
    }

    const payload = {
        title: workJobForm.elements.title.value,
        description: workJobForm.elements.description.value,
        openClawId: state.selectedPoolOpenClawId,
        desiredTags: splitTagText(workJobForm.elements.desiredTagText.value)
    };

    try {
        const result = await apiFetch("/api/work-jobs", {
            method: "POST",
            body: JSON.stringify(payload)
        });
        setFeedback(jobFeedback, `任务单“${result.title}”已创建。`, "success");
        workJobForm.reset();
        await loadWorkJobs(false);
    } catch (error) {
        setFeedback(jobFeedback, `创建任务单失败：${error.message}`, "error");
    }
}

async function submitLobster(event) {
    event.preventDefault();
    if (!state.token) {
        setFeedback(lobsterFeedback, "请先登录，再上传龙虾。", "error");
        return;
    }

    const formData = new FormData();
    formData.append("name", lobsterForm.elements.name.value);
    formData.append("description", lobsterForm.elements.description.value);
    formData.append("tagText", lobsterForm.elements.tagText.value);

    const file = lobsterForm.elements.file.files[0];
    if (!file) {
        setFeedback(lobsterFeedback, "请选择要上传的龙虾文件。", "error");
        return;
    }
    formData.append("file", file);

    try {
        const result = await apiFetch("/api/lobsters", {
            method: "POST",
            body: formData
        });
        setFeedback(lobsterFeedback, `龙虾“${result.name}”已上传。`, "success");
        lobsterForm.reset();
        await loadLobsters(false);
    } catch (error) {
        setFeedback(lobsterFeedback, `上传龙虾失败：${error.message}`, "error");
    }
}

// 使用事件委托处理我管理的实例卡片动作。
function handleManagedInstanceAction(event) {
    const actionButton = event.target.closest("[data-action]");
    if (!actionButton) {
        return;
    }

    const { action, id } = actionButton.dataset;
    if (action === "edit") {
        startEdit(id);
        return;
    }
    if (action === "delete") {
        deleteManagedOpenClaw(id);
    }
}

// 使用事件委托处理资源池的“选它干活”动作。
function handlePoolAction(event) {
    const actionButton = event.target.closest("[data-pool-action]");
    if (!actionButton) {
        return;
    }

    const { poolAction, id } = actionButton.dataset;
    if (poolAction === "select") {
        selectPoolOpenClaw(id);
    }
}

loginForm.addEventListener("submit", async (event) => {
    event.preventDefault();
    const formData = new FormData(loginForm);
    const payload = Object.fromEntries(formData.entries());

    try {
        const result = await apiFetch("/api/auth/login", {
            method: "POST",
            body: JSON.stringify(payload)
        });
        saveToken(result.token);
        setFeedback(authFeedback, `登录成功，欢迎回来：${result.username}`, "success");
        loginForm.reset();
        await loadAllAuthenticatedData();
    } catch (error) {
        setFeedback(authFeedback, `登录失败：${error.message}`, "error");
    }
});

registerForm.addEventListener("submit", async (event) => {
    event.preventDefault();
    const formData = new FormData(registerForm);
    const payload = Object.fromEntries(formData.entries());

    try {
        const result = await apiFetch("/api/auth/register", {
            method: "POST",
            body: JSON.stringify(payload)
        });
        saveToken(result.token);
        setFeedback(authFeedback, `注册成功，当前已登录为：${result.username}`, "success");
        registerForm.reset();
        await loadAllAuthenticatedData();
    } catch (error) {
        setFeedback(authFeedback, `注册失败：${error.message}`, "error");
    }
});

openClawForm.addEventListener("submit", submitInstanceForm);
workJobForm.addEventListener("submit", submitWorkJob);
lobsterForm.addEventListener("submit", submitLobster);
openClawList.addEventListener("click", handleManagedInstanceAction);
poolList.addEventListener("click", handlePoolAction);
loadProfileButton.addEventListener("click", () => loadProfile(true));
logoutButton.addEventListener("click", clearSession);
loadOpenClawsButton.addEventListener("click", () => loadManagedOpenClaws(true));
loadPoolButton.addEventListener("click", () => loadPoolOpenClaws(true));
loadWorkJobsButton.addEventListener("click", () => loadWorkJobs(true));
loadLobstersButton.addEventListener("click", () => loadLobsters(true));
cancelEditButton.addEventListener("click", () => {
    resetInstanceForm();
    setFeedback(resourceFeedback, "已退出编辑模式。", "success");
});
resetInstanceButton.addEventListener("click", () => {
    resetInstanceForm();
    setFeedback(resourceFeedback, "表单已清空。", "success");
});
clearSelectedOpenClawButton.addEventListener("click", () => {
    clearSelectedOpenClaw();
    setFeedback(jobFeedback, "已清空当前选择。", "success");
});

authModeButtons.forEach((button) => {
    button.addEventListener("click", () => switchAuthMode(button.dataset.authMode));
});

async function bootstrap() {
    switchAuthMode("login");
    renderProfile();
    renderSummary();
    renderManagedOpenClaws();
    renderPoolOpenClaws();
    renderWorkJobs();
    renderLobsters();
    renderSelectedOpenClaw();
    resetInstanceForm();

    if (state.token) {
        await loadAllAuthenticatedData();
    }
}

bootstrap();

const TOKEN_KEY = "clawpond.jwt";

const state = {
    token: localStorage.getItem(TOKEN_KEY) || "",
    profile: null,
    openclaws: []
};

const authFeedback = document.getElementById("auth-feedback");
const resourceFeedback = document.getElementById("resource-feedback");
const loginForm = document.getElementById("login-form");
const registerForm = document.getElementById("register-form");
const openClawForm = document.getElementById("openclaw-form");
const loadProfileButton = document.getElementById("load-profile-btn");
const logoutButton = document.getElementById("logout-btn");
const loadOpenClawsButton = document.getElementById("load-openclaws-btn");
const authModeButtons = document.querySelectorAll("[data-auth-mode]");

function setFeedback(element, message, type = "") {
    element.textContent = message || "";
    element.className = "feedback-panel";
    if (type) {
        element.classList.add(type);
    }
}

function switchAuthMode(mode) {
    authModeButtons.forEach((button) => {
        button.classList.toggle("active", button.dataset.authMode === mode);
    });
    loginForm.classList.toggle("hidden", mode !== "login");
    registerForm.classList.toggle("hidden", mode !== "register");
    setFeedback(authFeedback, "");
}

async function apiFetch(path, options = {}) {
    const headers = {
        ...(options.body ? { "Content-Type": "application/json" } : {}),
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
        throw new Error(extractErrorMessage(data) || `Request failed: ${response.status}`);
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
            .map(([field, message]) => `${field}: ${message}`)
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

function clearSession() {
    saveToken("");
    state.profile = null;
    state.openclaws = [];
    renderProfile();
    renderOpenClaws();
    setFeedback(authFeedback, "Signed out.");
    setFeedback(resourceFeedback, "");
}

function formatDate(value) {
    if (!value) {
        return "-";
    }
    return new Date(value).toLocaleString("en-CA", { hour12: false });
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

function renderOpenClaws() {
    const empty = document.getElementById("inventory-empty");
    const list = document.getElementById("openclaw-list");

    if (!state.openclaws.length) {
        empty.classList.remove("hidden");
        list.classList.add("hidden");
        list.innerHTML = "";
        return;
    }

    list.innerHTML = state.openclaws.map((item) => `
        <article class="instance-card">
            <div class="instance-head">
                <strong>${escapeHtml(item.name)}</strong>
                <span class="status-pill ${item.active ? "" : "inactive"}">${item.active ? "ACTIVE" : "INACTIVE"}</span>
            </div>
            <a class="instance-url" href="${escapeAttribute(item.baseUrl)}" target="_blank" rel="noreferrer">${escapeHtml(item.baseUrl)}</a>
            <p>${escapeHtml(item.description || "No description")}</p>
            <div class="instance-meta">
                <span>externalId: ${escapeHtml(item.externalId)}</span>
                <span>owner: ${escapeHtml(item.ownerUsername)}</span>
                <span>created: ${escapeHtml(formatDate(item.createdAt))}</span>
            </div>
        </article>
    `).join("");

    empty.classList.add("hidden");
    list.classList.remove("hidden");
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
            setFeedback(authFeedback, "Session refreshed.", "success");
        }
    } catch (error) {
        clearSession();
        setFeedback(authFeedback, `Session expired: ${error.message}`, "error");
    }
}

async function loadOpenClaws(showFeedback = false) {
    if (!state.token) {
        state.openclaws = [];
        renderOpenClaws();
        if (showFeedback) {
            setFeedback(resourceFeedback, "Sign in before loading inventory.", "error");
        }
        return;
    }

    try {
        state.openclaws = await apiFetch("/api/openclaws", { method: "GET" });
        renderOpenClaws();
        if (showFeedback) {
            setFeedback(resourceFeedback, `Loaded ${state.openclaws.length} instance(s).`, "success");
        }
    } catch (error) {
        setFeedback(resourceFeedback, `Inventory load failed: ${error.message}`, "error");
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
        setFeedback(authFeedback, `Signed in as ${result.username}.`, "success");
        loginForm.reset();
        await loadProfile(false);
        await loadOpenClaws(false);
    } catch (error) {
        setFeedback(authFeedback, `Sign in failed: ${error.message}`, "error");
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
        setFeedback(authFeedback, `Registered and signed in as ${result.username}.`, "success");
        registerForm.reset();
        await loadProfile(false);
        await loadOpenClaws(false);
    } catch (error) {
        setFeedback(authFeedback, `Registration failed: ${error.message}`, "error");
    }
});

openClawForm.addEventListener("submit", async (event) => {
    event.preventDefault();
    if (!state.token) {
        setFeedback(resourceFeedback, "Sign in before creating an instance.", "error");
        return;
    }

    const formData = new FormData(openClawForm);
    const payload = Object.fromEntries(formData.entries());

    try {
        const result = await apiFetch("/api/openclaws", {
            method: "POST",
            body: JSON.stringify(payload)
        });
        setFeedback(resourceFeedback, `Created instance ${result.name}.`, "success");
        openClawForm.reset();
        await loadOpenClaws(false);
    } catch (error) {
        setFeedback(resourceFeedback, `Create failed: ${error.message}`, "error");
    }
});

loadProfileButton.addEventListener("click", () => loadProfile(true));
logoutButton.addEventListener("click", clearSession);
loadOpenClawsButton.addEventListener("click", () => loadOpenClaws(true));

authModeButtons.forEach((button) => {
    button.addEventListener("click", () => switchAuthMode(button.dataset.authMode));
});

async function bootstrap() {
    switchAuthMode("login");
    renderProfile();
    renderOpenClaws();

    if (state.token) {
        await loadProfile(false);
        await loadOpenClaws(false);
    }
}

bootstrap();


import axios from "axios";
import router from "@/router";

const LOOPBACK_HOSTS = new Set(["localhost", "127.0.0.1", "::1"]);

// 优先使用环境变量中的API地址，如果没有则使用window对象上的配置，最后使用默认值
function resolveDefaultBaseURL() {
  if (typeof window !== "undefined" && window.location?.hostname) {
    return `${window.location.protocol}//${window.location.hostname}:8888`;
  }
  return "http://localhost:8888";
}

function normalizeLoopbackBaseURL(rawBaseURL?: string) {
  if (typeof window === "undefined" || !rawBaseURL) {
    return rawBaseURL || "";
  }

  try {
    const parsedUrl = new URL(rawBaseURL, window.location.origin);
    const currentHost = window.location.hostname;

    if (LOOPBACK_HOSTS.has(parsedUrl.hostname) && LOOPBACK_HOSTS.has(currentHost)) {
      parsedUrl.hostname = currentHost;
    }

    return parsedUrl.toString().replace(/\/$/, "");
  } catch (error) {
    void error;
    return rawBaseURL;
  }
}

function resolveRuntimeBaseURL() {
  if (typeof window === "undefined") {
    return "";
  }

  const queryValue = new URLSearchParams(window.location.search).get("apiBaseUrl");
  if (queryValue) {
    const normalizedQueryValue = normalizeLoopbackBaseURL(queryValue);
    try {
      window.localStorage?.setItem("LIGHT_MUSIC_API_BASE_URL", normalizedQueryValue);
    } catch (error) {
      void error;
    }
    return normalizedQueryValue;
  }

  try {
    const runtimeBaseURL = window.localStorage?.getItem("LIGHT_MUSIC_API_BASE_URL") || "";
    const normalizedRuntimeBaseURL = normalizeLoopbackBaseURL(runtimeBaseURL);

    if (normalizedRuntimeBaseURL && normalizedRuntimeBaseURL !== runtimeBaseURL) {
      window.localStorage?.setItem("LIGHT_MUSIC_API_BASE_URL", normalizedRuntimeBaseURL);
    }

    return normalizedRuntimeBaseURL;
  } catch (error) {
    return "";
  }
}

const BASE_URL =
  resolveRuntimeBaseURL() ||
  normalizeLoopbackBaseURL((window as any).VUE_APP_API_URL) ||
  normalizeLoopbackBaseURL(process.env.VUE_APP_API_URL) ||
  normalizeLoopbackBaseURL(process.env.NODE_HOST) ||
  resolveDefaultBaseURL();

axios.defaults.timeout = 5000;
axios.defaults.withCredentials = true;
axios.defaults.baseURL = BASE_URL;
axios.defaults.headers.post["Content-Type"] = "application/x-www-form-urlencoded;charset=UTF-8";

axios.interceptors.response.use(
  (response) => {
    if (response.status === 200) {
      return Promise.resolve(response);
    }
    return Promise.reject(response);
  },
  (error) => {
    if (error.response?.status) {
      switch (error.response.status) {
        case 401:
          router.replace({
            path: "/",
            query: {},
          });
          break;
        case 403:
          window.setTimeout(() => {
            router.replace({
              path: "/",
              query: {},
            });
          }, 1000);
          break;
        case 404:
          break;
      }
      return Promise.reject(error.response);
    }

    return Promise.reject(error);
  }
);

export function getBaseURL() {
  return BASE_URL;
}

export function get(url, params?: object) {
  return new Promise((resolve, reject) => {
    axios.get(url, params).then(
      (response) => resolve(response.data),
      (error) => reject(error)
    );
  });
}

export function post(url, data = {}) {
  return new Promise((resolve, reject) => {
    axios.post(url, data).then(
      (response) => resolve(response.data),
      (error) => reject(error)
    );
  });
}

export function postJson(url, data = {}, config = {}) {
  return new Promise((resolve, reject) => {
    axios.post(url, data, {
      ...config,
      headers: {
        "Content-Type": "application/json;charset=UTF-8",
        ...(config as any)?.headers,
      },
    }).then(
      (response) => resolve(response.data),
      (error) => reject(error)
    );
  });
}

export function deletes(url, data = {}) {
  return new Promise((resolve, reject) => {
    axios.delete(url, data).then(
      (response) => resolve(response.data),
      (error) => reject(error)
    );
  });
}

export function put(url, data = {}) {
  return new Promise((resolve, reject) => {
    axios.put(url, data).then(
      (response) => resolve(response.data),
      (error) => reject(error)
    );
  });
}

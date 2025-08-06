import axios, { AxiosInstance, AxiosRequestConfig, AxiosResponse } from "axios";

class ApiClient {
  private instance: AxiosInstance;

  constructor() {
    this.instance = axios.create({
      baseURL: process.env.NEXT_PUBLIC_API_BASE_URL || "",
      timeout: 10000,
      headers: {
        "Content-Type": "application/json",
      },
    });

    this.setupInterceptors();
  }

  private setupInterceptors() {
    // 요청 인터셉터: 자동으로 JWT 헤더 추가
    this.instance.interceptors.request.use(
      (config) => {
        if (typeof window !== "undefined") {
          const token = localStorage.getItem("accessToken");
          if (token) {
            config.headers.Authorization = `Bearer ${token}`;
          }
        }
        return config;
      },
      (error) => {
        console.error("Request interceptor error:", error);
        return Promise.reject(error);
      }
    );

    // 응답 인터셉터: 401 에러 시 토큰 갱신 시도 후 로그아웃
    this.instance.interceptors.response.use(
      (response) => response,
      async (error) => {
        if (error.response?.status === 401) {
          // 토큰 갱신 시도
          const refreshToken = localStorage.getItem("refreshToken");
          if (refreshToken) {
            try {
              // authApi의 refreshToken 함수 사용하면 순환참조 발생하므로 직접 호출
              const response = await fetch(
                `${process.env.NEXT_PUBLIC_API_BASE_URL}/api/auth/refresh`,
                {
                  method: "POST",
                  headers: { "Content-Type": "application/json" },
                  body: JSON.stringify({ refreshToken }),
                }
              );

              if (response.ok) {
                const data = await response.json();
                localStorage.setItem("accessToken", data.accessToken);
                if (data.refreshToken) {
                  localStorage.setItem("refreshToken", data.refreshToken);
                }

                // 원래 요청 재시도
                error.config.headers.Authorization = `Bearer ${data.accessToken}`;
                return this.instance.request(error.config);
              }
            } catch (refreshError) {
              console.error("Token refresh failed:", refreshError);
            }
          }

          // 토큰 갱신 실패 시 로그아웃
          localStorage.removeItem("accessToken");
          localStorage.removeItem("refreshToken");
          if (typeof window !== "undefined") {
            window.location.href = "/login";
          }
        }
        return Promise.reject(error);
      }
    );
  }

  // 기본값 없는 제네릭 - 타입을 반드시 명시해야 함
  get<T>(url: string, config?: AxiosRequestConfig): Promise<AxiosResponse<T>> {
    return this.instance.get<T>(url, config);
  }

  post<T>(
    url: string,
    data?: unknown,
    config?: AxiosRequestConfig
  ): Promise<AxiosResponse<T>> {
    return this.instance.post<T>(url, data, config);
  }

  put<T>(
    url: string,
    data?: unknown,
    config?: AxiosRequestConfig
  ): Promise<AxiosResponse<T>> {
    return this.instance.put<T>(url, data, config);
  }

  delete<T>(
    url: string,
    config?: AxiosRequestConfig
  ): Promise<AxiosResponse<T>> {
    return this.instance.delete<T>(url, config);
  }

  patch<T>(
    url: string,
    data?: unknown,
    config?: AxiosRequestConfig
  ): Promise<AxiosResponse<T>> {
    return this.instance.patch<T>(url, data, config);
  }
}

export const apiClient = new ApiClient();

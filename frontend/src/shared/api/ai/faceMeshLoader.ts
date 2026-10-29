import { withTimeout } from "./asyncResource";

type FaceMeshConstructor = typeof import("@mediapipe/face_mesh").FaceMesh;
type FaceMeshWindow = Window & {
  FaceMesh?: FaceMeshConstructor;
  Module?: unknown;
  createMediapipeSolutionsPackedAssets?: unknown;
};

export const FACE_MESH_ASSETS = "https://cdn.jsdelivr.net/npm/@mediapipe/face_mesh@0.4.1633559619";
let loading: Promise<FaceMeshConstructor> | null = null;

// This legacy package exposes a browser global, not an ESM named export.
// Both face processors share one versioned script; each owns its own instance.
export function loadFaceMesh(): Promise<FaceMeshConstructor> {
  const existing = (window as FaceMeshWindow).FaceMesh;
  if (existing) return Promise.resolve(existing);
  if (loading) return loading;
  const script = document.createElement("script");
  script.src = `${FACE_MESH_ASSETS}/face_mesh.js`;
  script.async = true;
  const task = new Promise<FaceMeshConstructor>((resolve, reject) => {
    script.onload = () => {
      const constructor = (window as FaceMeshWindow).FaceMesh;
      if (constructor) resolve(constructor);
      else reject(new Error("얼굴 인식 모듈을 불러오지 못했습니다."));
    };
    script.onerror = () => reject(new Error("얼굴 인식 모듈을 다운로드하지 못했습니다."));
    document.head.appendChild(script);
  });
  loading = withTimeout(task, 10000, "얼굴 인식 모듈 로딩 시간이 초과됐습니다.").catch((error) => {
    script.remove();
    loading = null;
    throw error;
  }).finally(() => { script.onload = null; script.onerror = null; });
  return loading;
}

// The legacy asset loader leaks its Emscripten Module onto window. Tasks Vision
// treats that value as its own configuration, so release it after loading ends.
export async function initializeFaceMesh(face: import("@mediapipe/face_mesh").FaceMesh): Promise<void> {
  try {
    await face.initialize();
  } finally {
    const globals = window as FaceMeshWindow;
    if (globals.Module === globals.createMediapipeSolutionsPackedAssets) globals.Module = undefined;
  }
}

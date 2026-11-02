/// <reference types="vite/client" />

interface ImportMetaEnv {
  /** 接口基础路径，默认 /api（走 Vite 代理） */
  readonly VITE_API_BASE?: string
}

interface ImportMeta {
  readonly env: ImportMetaEnv
}

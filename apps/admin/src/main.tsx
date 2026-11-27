import { StrictMode } from "react";
import { createRoot } from "react-dom/client";
import { App } from "./App";
import { readConfig } from "./config";
import "./styles.css";

const root = document.getElementById("root");
if (root) {
  createRoot(root).render(
    <StrictMode>
      <App config={readConfig(window.VIGIA_CONFIG)} />
    </StrictMode>,
  );
}

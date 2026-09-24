// Evita a janela de console adicional no Windows em release. NÃO REMOVER.
#![cfg_attr(not(debug_assertions), windows_subsystem = "windows")]

fn main() {
  compara_precos_desktop_lib::run();
}

# Game Tour

**EN** · [PT-BR](#português-brasil)

An open-source **game mode for custom Android ROMs**, written in Kotlin + Jetpack Compose with a "Liquid Glass" look. It started as a replacement for GameSpace on the **POCO X6 Pro / Redmi K70E (duchamp, MT6897)** and runs as a privileged, platform-signed system app.

> Status: working on AlphaDroid A16 (Android 16) for `duchamp`. Parts of it are device-specific (MediaTek paths), and it needs a few things from the ROM. See [PORTING.md](PORTING.md) before trying it on another ROM or device.

## Features

- Live **FPS** and **frame time** (1% low), **CPU %**, **GPU %**, GPU/CPU clocks, temperature, battery, power (W) and RAM, shown in a draggable on-screen label. You choose the metrics.
- **Edge handle** (Game Turbo style): pull it inward to open the in-game panel. It stays on screen as a translucent glass strip; long-press and drag to move it.
- **In-game panel** with blur: Do Not Disturb, screen record, screenshot, clean RAM, FPS toggle, charge bypass, spatial audio, brightness and a thermal profile (Balanced / Game / Performance).
- **Quick apps** that open in a **floating window** (through LMOFreeform).
- Per-game profiles, session **report** (average/min/max FPS, stability, drops, temperature, battery) and history.
- **Call handling** during a game (normal / auto-answer / auto-decline) and notification banners.
- Bluetooth codec swap for low latency while gaming, restored afterwards.
- 5 themes (Aurora, Dark, Cyberpunk, Ocean, GTA 6), light/dark aware.

## What the ROM must provide

| Requirement | Why |
|---|---|
| Platform signature + `sharedUserId=android.uid.system`, privileged app in `system_ext` | System permissions (FPS counter, secure settings, audio routing…) |
| `game_space` framework service (`IGameSpaceService` / `IGameSpaceCallback`) | Tells the app when a game starts and ends |
| `ax_platform_hooks` library (AxionOS) | Screenshot and screen record from the panel |
| LMOFreeform (`com.libremobileos.freeform`) — optional | Floating-window quick apps |
| SELinux rules (see `sepolicy/`) | Reading GPU/CPU stats and dumping SurfaceFlinger |

Full details and a checklist: **[PORTING.md](PORTING.md)**.

## Build

It is an AOSP app (`Android.bp`), not a Gradle project. Put the `app/` folder in your device tree, for example `device/<vendor>/<device>-apps/gametour`, and include `apps.mk` from your `device.mk`:

```make
$(call inherit-product-if-exists, device/<vendor>/<device>-apps/gametour/apps.mk)
```

Then build with `m GameTour`. Adapt `GAMETOUR_DIR` in `apps.mk` to your path.

## License and credits

Apache License 2.0, see [LICENSE](LICENSE).

### Credits are required

If you use, port, fork or redistribute Game Tour (including inside a ROM), you **must keep the [NOTICE](NOTICE) file and the copyright notice, and credit the original author: SeMpAz-guhzkx**. This is a condition of the Apache-2.0 license (section 4). A short line such as _Game Tour by SeMpAz-guhzkx_ in your ROM's credits or post is appreciated.

Third-party credits are listed in [NOTICE](NOTICE). Maintainer: **SeMpAz-guhzkx**.

---

## Português (Brasil)

Um **modo jogo de código aberto para ROMs custom Android**, em Kotlin + Jetpack Compose, com visual "Liquid Glass". Nasceu para substituir o GameSpace no **POCO X6 Pro / Redmi K70E (duchamp, MT6897)** e roda como app de sistema privilegiado, assinado com a chave platform.

> Estado: funcionando na AlphaDroid A16 (Android 16) para o `duchamp`. Parte do código é específica do aparelho (caminhos da MediaTek) e depende de itens da ROM. Leia o [PORTING.md](PORTING.md) antes de portar para outra ROM ou aparelho.

### Recursos

- **FPS**, tempo de quadro (1% low), **CPU %**, **GPU %**, clocks, temperatura, bateria, potência (W) e RAM, em um rótulo arrastável na tela. Você escolhe as medidas.
- **Alça de borda** (estilo Game Turbo): puxe para dentro para abrir o painel. Fica sempre visível como uma tira de vidro translúcida; segure e arraste para mudar de lugar.
- **Painel no jogo** com desfoque: Não perturbe, gravar tela, captura, limpar RAM, FPS, bypass de carga, áudio espacial, brilho e perfil térmico (Equilibrado / Jogo / Desempenho).
- **Apps rápidos** em **janela flutuante** (via LMOFreeform).
- Perfis por jogo, **relatório** da sessão (FPS médio/mín./máx., estabilidade, quedas, temperatura, bateria) e histórico.
- **Chamadas** durante o jogo (normal / atender / recusar) e banners de notificação.
- Troca do codec Bluetooth para baixa latência durante o jogo, restaurado ao sair.
- 5 temas (Aurora, Escuro, Cyberpunk, Oceano, GTA 6).

### O que a ROM precisa ter

Veja a tabela acima e o checklist completo no [PORTING.md](PORTING.md).

### Compilar

É um app AOSP (`Android.bp`), não um projeto Gradle. Coloque a pasta `app/` na sua árvore de device (por exemplo `device/<fabricante>/<aparelho>-apps/gametour`), inclua o `apps.mk` no seu `device.mk` e compile com `m GameTour`.

### Licença e créditos

Apache License 2.0 ([LICENSE](LICENSE)).

#### Créditos obrigatórios

Se você usar, portar, fazer fork ou redistribuir o Game Tour (inclusive dentro de uma ROM), **deve manter o arquivo [NOTICE](NOTICE) e o aviso de copyright, e dar crédito ao autor original: SeMpAz-guhzkx**. Isso é uma condição da licença Apache-2.0 (seção 4). Uma linha como _Game Tour por SeMpAz-guhzkx_ nos créditos da sua ROM ou no post é bem-vinda.

Créditos de terceiros em [NOTICE](NOTICE). Mantenedor: **SeMpAz-guhzkx**.

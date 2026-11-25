# Game Tour

[PT-BR](#português-brasil) · **EN**

Hey! This is **Game Tour**, my game mode for custom Android ROMs. I keep it for my own **POCO X6 Pro / Redmi K70E (duchamp)** and I'm leaving it open so anyone can use it or port it to another ROM.

I wanted a game mode that looks good and shows me what is really going on while I play: FPS, GPU, temperature, all on screen, and a panel I can pull out from the edge like Game Turbo. It replaces GameSpace on my ROM (AlphaDroid A16, Android 16), and I test everything on my own phone.

> Heads up: it is **not** a normal app you install from an APK. It is a system app, signed with the platform key, so it only works built inside a ROM. Read [PORTING.md](PORTING.md) before you try it somewhere else.

## What it does

- Live **FPS**, **frame time** (1% low), **CPU %**, **GPU %**, clocks, temperature, battery, power (W) and RAM, in a label you can drag around. You pick which ones to show.
- **Edge handle**: a thin glass strip on the screen edge. Pull it inward and the panel opens. Long-press and drag to move it.
- **In-game panel** (blurred background): Do Not Disturb, screen record, screenshot, clean RAM, FPS on/off, charge bypass, spatial audio, brightness and thermal profile (Balanced / Game / Performance).
- **Quick apps** that open in a floating window, so you can answer something without leaving the game.
- Profile for each game, a **report** when the match ends (avg/min/max FPS, stability, drops, temp, battery) and a history.
- **Calls** while gaming: normal, auto-answer or auto-decline, plus notification banners.
- Switches the Bluetooth codec to low latency while you play and puts it back after.
- 5 themes: Aurora, Dark, Cyberpunk, Ocean and GTA 6.

## What your ROM needs

| Needs | Why |
|---|---|
| Platform signature, `sharedUserId=android.uid.system`, privileged app in `system_ext` | System permissions (FPS counter, secure settings, audio routing...) |
| `game_space` service in the framework (`IGameSpaceService` / `IGameSpaceCallback`) | Tells the app when a game starts and ends |
| `ax_platform_hooks` (AxionOS) | Screenshot and screen record from the panel |
| LMOFreeform (`com.libremobileos.freeform`), optional | Floating window for the quick apps |
| SELinux rules (folder `sepolicy/`) | Read GPU/CPU stats and the SurfaceFlinger dump |

All the details and a checklist are in **[PORTING.md](PORTING.md)**.

## How to build

It is an AOSP app (`Android.bp`), not Gradle. Drop the `app/` folder in your device tree (for example `device/<vendor>/<device>-apps/gametour`), include the `apps.mk` from your `device.mk` and build with `m GameTour`:

```make
$(call inherit-product-if-exists, device/<vendor>/<device>-apps/gametour/apps.mk)
```

Change `GAMETOUR_DIR` in `apps.mk` to match your path.

## Credits (please keep them)

Apache License 2.0, see [LICENSE](LICENSE).

If you use it, port it, fork it or ship it inside a ROM, please **keep the [NOTICE](NOTICE) file and credit me: SeMpAz-guhzkx**. It costs nothing, and it is also a condition of the license (section 4). A simple _Game Tour by SeMpAz-guhzkx_ in your ROM credits or post is perfect.

Credits to the other projects I learned from and adapted parts of are in [NOTICE](NOTICE). If I forgot anyone, open an issue and I fix it.

---

## Português (Brasil)

Fala, galera! Esse é o **Game Tour**, o meu modo jogo para ROM custom. Eu mantenho ele para o meu **POCO X6 Pro / Redmi K70E (duchamp)** e estou deixando aberto para quem quiser usar ou portar para outra ROM.

Eu queria um modo jogo bonito e que mostrasse o que está acontecendo de verdade enquanto eu jogo: FPS, GPU, temperatura, tudo na tela, e um painel que abre puxando da borda, igual o Game Turbo. Ele substitui o GameSpace na minha ROM (AlphaDroid A16, Android 16), e eu testo tudo no meu aparelho.

> Atenção: **não** é um app normal de instalar por APK. É app de sistema, assinado com a chave platform, então só funciona compilado dentro de uma ROM. Lê o [PORTING.md](PORTING.md) antes de tentar em outro lugar.

### O que ele faz

- **FPS**, **tempo de quadro** (1% low), **CPU %**, **GPU %**, clocks, temperatura, bateria, potência (W) e RAM, num rótulo que você arrasta pela tela. Você escolhe o que mostrar.
- **Alça de borda**: uma tirinha de vidro na lateral. Puxa para dentro e o painel abre. Segura e arrasta para mudar de lugar.
- **Painel no jogo** (fundo desfocado): Não perturbe, gravar tela, captura, limpar RAM, FPS, bypass de carga, áudio espacial, brilho e perfil térmico (Equilibrado / Jogo / Desempenho).
- **Apps rápidos** que abrem em janela flutuante, para responder algo sem sair do jogo.
- Perfil de cada jogo, **relatório** quando a partida acaba (FPS médio/mín./máx., estabilidade, quedas, temperatura, bateria) e histórico.
- **Chamadas** durante o jogo: normal, atender ou recusar sozinho, e banners de notificação.
- Troca o codec Bluetooth para baixa latência enquanto você joga e volta ao normal depois.
- 5 temas: Aurora, Escuro, Cyberpunk, Oceano e GTA 6.

### O que a sua ROM precisa ter

Mesma tabela de cima. O passo a passo completo e um checklist estão no **[PORTING.md](PORTING.md)**.

### Como compilar

É app AOSP (`Android.bp`), não é Gradle. Coloca a pasta `app/` na sua árvore de device (por exemplo `device/<fabricante>/<aparelho>-apps/gametour`), inclui o `apps.mk` no seu `device.mk` e compila com `m GameTour`.

### Créditos (deixa lá, por favor)

Licença Apache 2.0, veja o [LICENSE](LICENSE).

Se for usar, portar, fazer fork ou colocar dentro de uma ROM, **mantém o arquivo [NOTICE](NOTICE) e dá o crédito para mim: SeMpAz-guhzkx**. Não custa nada, e ainda é uma condição da licença (seção 4). Uma linha tipo _Game Tour por SeMpAz-guhzkx_ nos créditos da sua ROM ou no post já está ótimo.

Os créditos dos outros projetos que eu estudei e de onde adaptei partes estão no [NOTICE](NOTICE). Se eu esqueci de alguém, abre uma issue que eu corrijo.

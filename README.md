# MobDropEditor

**MobDropEditor** é um plugin Bukkit / Paper / Folia de alta performance para Minecraft, projetado para permitir a configuração completa dos drops de mobs através de uma GUI interativa no jogo.

---

## Recursos Principais

- **GUI Interativa Completa**:
  - Lista paginada de todos os mobs detectados.
  - Filtro e pesquisa por nome/ID.
  - Edição de chances com precisão decimal (ex: `50%`, `0.5%`, `0.01%`).
  - Configuração de quantidades mínima e máxima por drop.
  - Adição rápida do item que o jogador está segurando na mão principal.
  - Modos de drop configuráveis por mob: `VANILLA + CUSTOM`, `CUSTOM ONLY`, `VANILLA ONLY`.
- **Compatibilidade com Folia**:
  - Totalmente compatível com a arquitetura region-threaded do Folia.
  - Utiliza schedulers adaptativos por entidade/região e tarefas assíncronas sem causar congelamentos ou race conditions.
- **Suporte Multiversão & NBT/Custom Items**:
  - Preserva ItemStack completo (Name, Lore, Enchantments, CustomModelData, Flags, Unbreakable, PersistentDataContainer / NBT Data, e Componentes).
  - Serialização inteligente com fallback seguro em Base64 e reconstrução em runtime.
- **Detecção Dinâmica de Mobs**:
  - Suporta Mobs Vanilla do Minecraft.
  - Detecta automaticamente mobs customizados e mobs registrados por mods/plugins utilizando identificadores no formato `namespace:id` (ex: `minecraft:zombie`, `modid:custom_mob`).
- **Persistência Flexível & Segura**:
  - Suporte a armazenamento em **YAML** (com salvamento atômico para evitar corrupção de arquivos) e **SQLite**.
- **Comandos & Permissões**:
  - `/mobdrops open [mob]` - Abre a GUI principal ou de um mob específico.
  - `/mobdrops add <mob> [chance] [min] [max]` - Adiciona um drop para o mob.
  - `/mobdrops remove <mob> <id>` - Remove um drop por ID.
  - `/mobdrops copy <origem> <destino>` - Copia as configurações de um mob para outro.
  - `/mobdrops reset <mob>` - Restaura a configuração padrão do mob.
  - `/mobdrops reload` - Recarrega as configurações.
  - `/mobdrops save` - Força o salvamento dos dados no banco/arquivo.
  - Permissões: `mobdrops.admin`, `mobdrops.reload`, `mobdrops.edit`, `mobdrops.view`.

---

## Requisitos e Instalação

### Requisitos
- Java 17 ou superior.
- Servidor Bukkit, Spigot, Paper ou Folia (Minecraft 1.12.2 até 1.20+ / 26.x).

### Instalação
1. Baixe o arquivo `MobDropEditor-1.0.0.jar` compilado.
2. Coloque-o na pasta `plugins/` do seu servidor.
3. Inicie ou reinicie o servidor.
4. Edite as configurações na pasta `plugins/MobDropEditor/` ou use a GUI no jogo `/mobdrops`.

---

## Estrutura de Arquivos de Configuração

### `config.yml`
```yaml
storage:
  type: "YAML" # Opções: YAML, SQLITE
  sqlite:
    file: "database.db"

messages:
  prefix: "&8[&6MobDrops&8] "
  no-permission: "&cVocê não possui permissão para executar este comando."
  reloaded: "&aConfiguração recarregada com sucesso!"
  saved: "&aConfiguração salva com sucesso!"
  mob-not-found: "&cMob '%mob%' não foi encontrado."
  drop-added: "&aDrop adicionado para %mob%!"
  drop-removed: "&cDrop %id% removido de %mob%."
```

### Exemplo de Armazenamento (`drops.yml`)
```yaml
mobs:
  minecraft:zombie:
    drop-mode: "VANILLA_AND_CUSTOM"
    drops:
      - id: "fd3a12b4"
        chance: 50.0
        min: 1
        max: 3
        enabled: true
        item:
          material: "ROTTEN_FLESH"
          amount: 1
      - id: "a8190c12"
        chance: 0.5
        min: 1
        max: 1
        enabled: true
        item:
          material: "DIAMOND_SWORD"
          amount: 1
          display-name: "§6Espada Lendária"
          lore:
            - "§7Drop ultra raro de Zombie"
          custom-model-data: 1001
```

---

## Folia & Compatibilidade

- **Scheduler Abstraction**: O plugin detecta automaticamente a presença das classes do Folia (`RegionizedServer`) no classpath. Quando executado em servidores Folia, redireciona o agendamento de tarefas para `GlobalRegionScheduler`, `RegionScheduler` e `EntityScheduler`.
- **Thread Safety**: Operações de leitura/escrita no mapa de drops usam coleções concorrentes (`ConcurrentHashMap`) e travas sincronizadas no armazenamento em disco/banco de dados.

---

## Testes & Compilação

Para compilar o projeto e executar os testes unitários automatizados:

```bash
# Compilar e executar testes
./gradlew build
```

---

## Limitações Conhecidas e Notas

- Em servidores modded híbridos (ex: Mohist/Arclight em versões antigas), a resolução de entidades customizadas depende do suporte da ponte Bukkit/Forge fornecida por essa plataforma. O plugin utiliza consultas reflexivas e de chave namespaced para máxima compatibilidade sem acoplamento direto com NMS.

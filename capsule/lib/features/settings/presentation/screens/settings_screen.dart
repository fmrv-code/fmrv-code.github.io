import 'dart:io';

import 'package:file_picker/file_picker.dart';
import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../../../../core/providers/database_provider.dart';
import '../../../../../core/providers/theme_provider.dart';
import '../../../../../core/services/backup_service.dart';
import '../../../../../core/theme/app_text_styles.dart';

class SettingsScreen extends ConsumerWidget {
  const SettingsScreen({super.key});

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final cs = Theme.of(context).colorScheme;
    final themeMode = ref.watch(themeProvider);

    return Scaffold(
      body: CustomScrollView(
        slivers: [
          SliverAppBar.large(
            title: const Text('Réglages'),
            titleTextStyle: AppTextStyles.largeTitle.copyWith(
              color: cs.onSurface,
              fontWeight: FontWeight.w700,
            ),
          ),
          SliverPadding(
            padding: const EdgeInsets.symmetric(horizontal: 20, vertical: 8),
            sliver: SliverList.list(
              children: [
                // ── Apparence ─────────────────────────────────────────────
                _SectionLabel('Apparence'),
                _Card(children: [
                  Padding(
                    padding: const EdgeInsets.fromLTRB(20, 16, 20, 16),
                    child: Column(
                      crossAxisAlignment: CrossAxisAlignment.start,
                      children: [
                        Text('Thème',
                            style: AppTextStyles.subheadline
                                .copyWith(color: cs.onSurfaceVariant)),
                        const SizedBox(height: 12),
                        SegmentedButton<ThemeMode>(
                          segments: const [
                            ButtonSegment(
                                value: ThemeMode.system,
                                icon: Icon(Icons.brightness_auto_outlined),
                                label: Text('Auto')),
                            ButtonSegment(
                                value: ThemeMode.light,
                                icon: Icon(Icons.light_mode_outlined),
                                label: Text('Clair')),
                            ButtonSegment(
                                value: ThemeMode.dark,
                                icon: Icon(Icons.dark_mode_outlined),
                                label: Text('Sombre')),
                          ],
                          selected: {themeMode},
                          onSelectionChanged: (s) => ref
                              .read(themeProvider.notifier)
                              .setTheme(s.first),
                          style: const ButtonStyle(
                              tapTargetSize: MaterialTapTargetSize.shrinkWrap),
                        ),
                      ],
                    ),
                  ),
                ]),
                const SizedBox(height: 32),

                // ── Synchronisation ───────────────────────────────────────
                _SectionLabel('Synchronisation'),
                _Card(children: [
                  _Tile(
                    icon: Icons.upload_rounded,
                    iconColor: cs.primary,
                    title: 'Exporter la sauvegarde',
                    subtitle: 'Partage un fichier JSON avec toutes tes données.',
                    onTap: () => _export(context, ref),
                  ),
                  _Div(),
                  _Tile(
                    icon: Icons.download_rounded,
                    iconColor: cs.primary,
                    title: 'Importer une sauvegarde',
                    subtitle: 'Restaure depuis un fichier JSON exporté.',
                    onTap: () => _import(context, ref),
                  ),
                  _Div(),
                  _SyncthingTile(cs: cs, ref: ref),
                ]),
                const SizedBox(height: 32),

                // ── Vie privée ────────────────────────────────────────────
                _SectionLabel('Vie privée'),
                _Card(children: [
                  _Tile(
                    icon: Icons.shield_outlined,
                    iconColor: cs.primary,
                    title: 'Stockage 100 % local',
                    subtitle: 'Tes données ne quittent jamais cet appareil.',
                    trailing: Icon(Icons.check_circle_rounded,
                        color: Colors.green.shade400, size: 20),
                  ),
                  _Div(),
                  _Tile(
                    icon: Icons.visibility_off_outlined,
                    iconColor: cs.primary,
                    title: 'Zéro tracking',
                    subtitle: 'Aucune télémétrie, aucun compte requis.',
                    trailing: Icon(Icons.check_circle_rounded,
                        color: Colors.green.shade400, size: 20),
                  ),
                ]),
                const SizedBox(height: 32),

                // ── À propos ──────────────────────────────────────────────
                _SectionLabel('À propos'),
                _Card(children: [
                  _Tile(
                    icon: Icons.info_outline_rounded,
                    title: 'Version',
                    trailing: Text('1.0.0',
                        style: AppTextStyles.callout
                            .copyWith(color: cs.onSurfaceVariant)),
                  ),
                  _Div(),
                  _Tile(
                    icon: Icons.code_rounded,
                    title: 'Open-source',
                    subtitle: 'MIT License',
                    trailing: Icon(Icons.open_in_new,
                        size: 15, color: cs.onSurfaceVariant),
                  ),
                ]),
                const SizedBox(height: 48),
              ],
            ),
          ),
        ],
      ),
    );
  }

  // ── Actions backup ──────────────────────────────────────────────────────────

  Future<void> _export(BuildContext context, WidgetRef ref) async {
    try {
      final service = BackupService(ref.read(databaseProvider));
      await service.exportAndShare();
    } catch (e) {
      if (context.mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          SnackBar(content: Text('Erreur export : $e')),
        );
      }
    }
  }

  Future<void> _import(BuildContext context, WidgetRef ref) async {
    final confirmed = await showDialog<bool>(
      context: context,
      builder: (ctx) => AlertDialog(
        title: const Text('Importer une sauvegarde'),
        content: const Text(
            'Les données existantes seront fusionnées avec la sauvegarde. Continue ?'),
        actions: [
          TextButton(
              onPressed: () => Navigator.pop(ctx, false),
              child: const Text('Annuler')),
          FilledButton(
              onPressed: () => Navigator.pop(ctx, true),
              child: const Text('Importer')),
        ],
      ),
    );

    if (confirmed != true || !context.mounted) return;

    try {
      final result = await FilePicker.platform.pickFiles(
        type: FileType.custom,
        allowedExtensions: ['json'],
      );
      if (result == null || !context.mounted) return;

      final file = File(result.files.single.path!);
      final json = await file.readAsString();
      final service = BackupService(ref.read(databaseProvider));
      final count = await service.importFromJson(json);

      if (context.mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          SnackBar(content: Text('$count notes importées avec succès.')),
        );
      }
    } catch (e) {
      if (context.mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          SnackBar(content: Text('Erreur import : $e')),
        );
      }
    }
  }
}

// ── Widgets internes ──────────────────────────────────────────────────────────

class _SyncthingTile extends StatelessWidget {
  const _SyncthingTile({required this.cs, required this.ref});
  final ColorScheme cs;
  final WidgetRef ref;

  @override
  Widget build(BuildContext context) {
    return FutureBuilder<String?>(
      future: BackupService(ref.read(databaseProvider)).syncFolderPath,
      builder: (context, snapshot) {
        final path = snapshot.data;
        return _Tile(
          icon: Icons.sync_rounded,
          iconColor: cs.primary,
          title: 'Sync automatique avec le PC',
          subtitle: path != null
              ? 'Dossier : $path'
              : 'Synchronise avec Syncthing.',
          trailing: Icon(Icons.info_outline_rounded,
              size: 18, color: cs.onSurfaceVariant),
          onTap: () => _showSyncDialog(context, path),
        );
      },
    );
  }

  void _showSyncDialog(BuildContext context, String? folderPath) {
    showDialog<void>(
      context: context,
      builder: (ctx) => AlertDialog(
        title: const Text('Sync avec le PC'),
        content: SingleChildScrollView(
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            mainAxisSize: MainAxisSize.min,
            children: [
              const Text(
                'Capsule sauvegarde automatiquement tes données dans un fichier JSON à chaque note enregistrée.',
              ),
              if (folderPath != null) ...[
                const SizedBox(height: 16),
                Container(
                  padding: const EdgeInsets.all(12),
                  decoration: BoxDecoration(
                    color: Theme.of(ctx).colorScheme.surfaceContainerHighest,
                    borderRadius: BorderRadius.circular(10),
                  ),
                  child: Row(
                    children: [
                      Expanded(
                        child: Text(
                          folderPath,
                          style: const TextStyle(
                              fontFamily: 'monospace', fontSize: 12),
                        ),
                      ),
                      IconButton(
                        icon: const Icon(Icons.copy_rounded, size: 18),
                        tooltip: 'Copier',
                        onPressed: () {
                          Clipboard.setData(
                              ClipboardData(text: folderPath));
                          Navigator.pop(ctx);
                          ScaffoldMessenger.of(context).showSnackBar(
                            const SnackBar(
                                content: Text('Chemin copié.')),
                          );
                        },
                      ),
                    ],
                  ),
                ),
              ],
              const SizedBox(height: 16),
              const Text(
                'Comment synchroniser avec ton PC :\n\n'
                '① Installe Syncthing-Fork (F-Droid) sur le téléphone\n'
                '② Installe Syncthing sur le PC (syncthing.net)\n'
                '③ Dans Syncthing Android :\n'
                '   Réglages → Accès à tous les fichiers → Autoriser\n'
                '④ Ajoute le dossier ci-dessus dans Syncthing\n'
                '⑤ Partage-le avec ton PC\n\n'
                'Capsule met à jour capsule_backup.json à chaque note sauvegardée.\n\n'
                'Sur le PC, utilise "Importer une sauvegarde" dans Réglages pour charger tes notes.',
              ),
            ],
          ),
        ),
        actions: [
          FilledButton(
            onPressed: () => Navigator.pop(ctx),
            child: const Text('Compris'),
          ),
        ],
      ),
    );
  }
}

class _SectionLabel extends StatelessWidget {
  const _SectionLabel(this.label);
  final String label;

  @override
  Widget build(BuildContext context) => Padding(
        padding: const EdgeInsets.only(left: 4, bottom: 8),
        child: Text(
          label.toUpperCase(),
          style: AppTextStyles.caption.copyWith(
            color: Theme.of(context).colorScheme.onSurfaceVariant,
            fontWeight: FontWeight.w600,
            letterSpacing: 0.8,
          ),
        ),
      );
}

class _Card extends StatelessWidget {
  const _Card({required this.children});
  final List<Widget> children;

  @override
  Widget build(BuildContext context) {
    final cs = Theme.of(context).colorScheme;
    return Container(
      decoration: BoxDecoration(
        color: cs.surface,
        borderRadius: BorderRadius.circular(20),
        border: Border.all(color: cs.outline, width: 1),
      ),
      clipBehavior: Clip.hardEdge,
      child: Column(mainAxisSize: MainAxisSize.min, children: children),
    );
  }
}

class _Tile extends StatelessWidget {
  const _Tile({
    required this.icon,
    required this.title,
    this.iconColor,
    this.subtitle,
    this.trailing,
    this.onTap,
  });

  final IconData icon;
  final Color? iconColor;
  final String title;
  final String? subtitle;
  final Widget? trailing;
  final VoidCallback? onTap;

  @override
  Widget build(BuildContext context) {
    final cs = Theme.of(context).colorScheme;
    return InkWell(
      onTap: onTap,
      child: Padding(
        padding: const EdgeInsets.symmetric(horizontal: 20, vertical: 14),
        child: Row(
          children: [
            Icon(icon, size: 20, color: iconColor ?? cs.onSurfaceVariant),
            const SizedBox(width: 14),
            Expanded(
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Text(title,
                      style: AppTextStyles.callout
                          .copyWith(color: cs.onSurface)),
                  if (subtitle != null) ...[
                    const SizedBox(height: 2),
                    Text(subtitle!,
                        style: AppTextStyles.caption
                            .copyWith(color: cs.onSurfaceVariant)),
                  ],
                ],
              ),
            ),
            if (trailing != null) ...[const SizedBox(width: 8), trailing!],
          ],
        ),
      ),
    );
  }
}

class _Div extends StatelessWidget {
  @override
  Widget build(BuildContext context) => Divider(
      height: 1,
      thickness: 1,
      indent: 54,
      color: Theme.of(context).colorScheme.outline);
}

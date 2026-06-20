import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:flutter_markdown/flutter_markdown.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';

import '../../../../../core/providers/database_provider.dart';
import '../../../../../core/services/backup_service.dart';
import '../../../../../core/theme/app_text_styles.dart';
import '../../../../../data/datasources/app_database.dart';
import '../widgets/markdown_toolbar.dart';

class NoteEditorScreen extends ConsumerStatefulWidget {
  const NoteEditorScreen({super.key, this.noteId});

  final int? noteId;

  @override
  ConsumerState<NoteEditorScreen> createState() => _NoteEditorScreenState();
}

class _NoteEditorScreenState extends ConsumerState<NoteEditorScreen> {
  final _titleCtrl = TextEditingController();
  final _contentCtrl = TextEditingController();
  final _titleFocus = FocusNode();
  final _contentFocus = FocusNode();

  Note? _existing;
  bool _focusMode = false;
  bool _previewMode = false;
  bool _saving = false;

  @override
  void initState() {
    super.initState();
    if (widget.noteId != null) _loadNote();
  }

  Future<void> _loadNote() async {
    final note = await ref.read(noteRepositoryProvider).getById(widget.noteId!);
    if (note != null && mounted) {
      setState(() {
        _existing = note;
        _titleCtrl.text = note.title;
        _contentCtrl.text = note.content;
      });
    }
  }

  Future<void> _save() async {
    final title = _titleCtrl.text.trim();
    final content = _contentCtrl.text;

    if (title.isEmpty && content.trim().isEmpty) {
      if (mounted) context.pop();
      return;
    }

    setState(() => _saving = true);
    final now = DateTime.now();

    final note = Note(
      id: _existing?.id ?? 0,
      title: title.isEmpty ? 'Sans titre' : title,
      content: content,
      capsuleId: _existing?.capsuleId,
      tagIds: _existing?.tagIds ?? const [],
      isPinned: _existing?.isPinned ?? false,
      isFavorite: _existing?.isFavorite ?? false,
      createdAt: _existing?.createdAt ?? now,
      updatedAt: now,
    );

    await ref.read(noteRepositoryProvider).save(note);
    BackupService(ref.read(databaseProvider)).autoBackup();
    if (mounted) context.pop();
  }

  void _toggleFocusMode() {
    setState(() {
      _focusMode = !_focusMode;
      if (_focusMode) _previewMode = false;
    });
    if (_focusMode) {
      SystemChrome.setEnabledSystemUIMode(SystemUiMode.immersiveSticky);
      _contentFocus.requestFocus();
    } else {
      SystemChrome.setEnabledSystemUIMode(SystemUiMode.edgeToEdge);
    }
  }

  void _togglePreview() {
    setState(() => _previewMode = !_previewMode);
    if (!_previewMode) {
      Future.delayed(const Duration(milliseconds: 50),
          () => _contentFocus.requestFocus());
    }
  }

  @override
  void dispose() {
    SystemChrome.setEnabledSystemUIMode(SystemUiMode.edgeToEdge);
    _titleCtrl.dispose();
    _contentCtrl.dispose();
    _titleFocus.dispose();
    _contentFocus.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    final cs = Theme.of(context).colorScheme;

    return PopScope(
      canPop: false,
      onPopInvokedWithResult: (didPop, _) async {
        if (!didPop) await _save();
      },
      child: Scaffold(
        backgroundColor: cs.surface,
        appBar: _focusMode ? null : _buildAppBar(cs),
        body: SafeArea(
          child: Column(
            children: [
              Expanded(
                child: Stack(
                  children: [
                    AnimatedSwitcher(
                      duration: const Duration(milliseconds: 220),
                      transitionBuilder: (child, anim) => FadeTransition(
                        opacity: anim,
                        child: child,
                      ),
                      child: _previewMode
                          ? _buildPreview(cs)
                          : _buildEditor(cs),
                    ),
                    if (_focusMode) _buildFocusExitButton(cs),
                  ],
                ),
              ),
              if (!_focusMode && !_previewMode)
                MarkdownToolbar(
                  controller: _contentCtrl,
                  focusNode: _contentFocus,
                ),
            ],
          ),
        ),
      ),
    );
  }

  PreferredSizeWidget _buildAppBar(ColorScheme cs) => AppBar(
        backgroundColor: cs.surface,
        scrolledUnderElevation: 0,
        leading: IconButton(
          icon: const Icon(Icons.arrow_back_ios_new_rounded),
          onPressed: _save,
          tooltip: 'Sauvegarder',
        ),
        actions: [
          // Aperçu / Édition
          IconButton(
            icon: Icon(
              _previewMode
                  ? Icons.edit_note_rounded
                  : Icons.chrome_reader_mode_outlined,
            ),
            tooltip: _previewMode ? 'Éditer' : 'Aperçu',
            onPressed: _togglePreview,
          ),
          // Focus
          if (!_previewMode)
            IconButton(
              icon: const Icon(Icons.fullscreen_rounded),
              tooltip: 'Mode Focus',
              onPressed: _toggleFocusMode,
            ),
          // Sauvegarder
          Padding(
            padding: const EdgeInsets.only(right: 12),
            child: _saving
                ? const SizedBox(
                    width: 20,
                    height: 20,
                    child:
                        CircularProgressIndicator.adaptive(strokeWidth: 2),
                  )
                : TextButton(
                    onPressed: _save,
                    child: Text(
                      'Terminé',
                      style: AppTextStyles.callout.copyWith(
                        color: cs.primary,
                        fontWeight: FontWeight.w600,
                      ),
                    ),
                  ),
          ),
        ],
      );

  // ── Éditeur ───────────────────────────────────────────────────────────────

  Widget _buildEditor(ColorScheme cs) => SingleChildScrollView(
        key: const ValueKey('editor'),
        padding: const EdgeInsets.fromLTRB(24, 16, 24, 48),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            TextField(
              controller: _titleCtrl,
              focusNode: _titleFocus,
              style: AppTextStyles.title1.copyWith(
                color: cs.onSurface,
                fontWeight: FontWeight.w700,
              ),
              decoration: InputDecoration(
                hintText: 'Titre',
                hintStyle: AppTextStyles.title1.copyWith(
                  color: cs.onSurfaceVariant.withOpacity(0.4),
                  fontWeight: FontWeight.w700,
                ),
                border: InputBorder.none,
                enabledBorder: InputBorder.none,
                focusedBorder: InputBorder.none,
                filled: false,
                contentPadding: EdgeInsets.zero,
              ),
              textCapitalization: TextCapitalization.sentences,
              onSubmitted: (_) => _contentFocus.requestFocus(),
            ),
            const SizedBox(height: 4),
            Divider(color: cs.outline, thickness: 1, height: 1),
            const SizedBox(height: 16),
            TextField(
              controller: _contentCtrl,
              focusNode: _contentFocus,
              style: AppTextStyles.body.copyWith(
                color: cs.onSurface,
                height: 1.65,
              ),
              decoration: InputDecoration(
                hintText: 'Commence à écrire…',
                hintStyle: AppTextStyles.body.copyWith(
                  color: cs.onSurfaceVariant.withOpacity(0.4),
                  height: 1.65,
                ),
                border: InputBorder.none,
                enabledBorder: InputBorder.none,
                focusedBorder: InputBorder.none,
                filled: false,
                contentPadding: EdgeInsets.zero,
              ),
              maxLines: null,
              minLines: 20,
              keyboardType: TextInputType.multiline,
              textCapitalization: TextCapitalization.sentences,
            ),
          ],
        ),
      );

  // ── Aperçu Markdown ───────────────────────────────────────────────────────

  Widget _buildPreview(ColorScheme cs) {
    final title = _titleCtrl.text;
    final content = _contentCtrl.text;

    return SingleChildScrollView(
      key: const ValueKey('preview'),
      padding: const EdgeInsets.fromLTRB(24, 16, 24, 48),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          if (title.isNotEmpty) ...[
            Text(
              title,
              style: AppTextStyles.title1.copyWith(
                color: cs.onSurface,
                fontWeight: FontWeight.w700,
              ),
            ),
            const SizedBox(height: 4),
            Divider(color: cs.outline, thickness: 1, height: 1),
            const SizedBox(height: 16),
          ],
          content.trim().isEmpty
              ? Text(
                  'Aucun contenu à afficher.',
                  style: AppTextStyles.body
                      .copyWith(color: cs.onSurfaceVariant.withOpacity(0.5)),
                )
              : MarkdownBody(
                  data: content,
                  selectable: true,
                  styleSheet: _markdownStyle(cs),
                ),
        ],
      ),
    );
  }

  MarkdownStyleSheet _markdownStyle(ColorScheme cs) => MarkdownStyleSheet(
        p: AppTextStyles.body.copyWith(color: cs.onSurface, height: 1.7),
        h1: AppTextStyles.title1.copyWith(
            color: cs.onSurface, fontWeight: FontWeight.w700),
        h2: AppTextStyles.title2.copyWith(
            color: cs.onSurface, fontWeight: FontWeight.w700),
        h3: AppTextStyles.title3.copyWith(
            color: cs.onSurface, fontWeight: FontWeight.w600),
        h4: AppTextStyles.headline.copyWith(
            color: cs.onSurface, fontWeight: FontWeight.w600),
        strong: AppTextStyles.body.copyWith(
            fontWeight: FontWeight.w700, color: cs.onSurface),
        em: AppTextStyles.body.copyWith(
            fontStyle: FontStyle.italic, color: cs.onSurface),
        del: AppTextStyles.body.copyWith(
            decoration: TextDecoration.lineThrough,
            color: cs.onSurfaceVariant),
        code: TextStyle(
          fontFamily: 'monospace',
          fontSize: 14,
          color: cs.primary,
          backgroundColor: cs.primaryContainer.withOpacity(0.25),
        ),
        codeblockPadding: const EdgeInsets.all(16),
        codeblockDecoration: BoxDecoration(
          color: cs.surfaceContainerHighest,
          borderRadius: BorderRadius.circular(12),
          border: Border.all(color: cs.outline),
        ),
        blockquotePadding:
            const EdgeInsets.symmetric(horizontal: 16, vertical: 10),
        blockquoteDecoration: BoxDecoration(
          border: Border(left: BorderSide(color: cs.primary, width: 3)),
          color: cs.primary.withOpacity(0.06),
          borderRadius: const BorderRadius.only(
            topRight: Radius.circular(8),
            bottomRight: Radius.circular(8),
          ),
        ),
        tableHead: AppTextStyles.callout.copyWith(
            fontWeight: FontWeight.w700, color: cs.onSurface),
        tableBody:
            AppTextStyles.body.copyWith(color: cs.onSurface, height: 1.5),
        tableBorder: TableBorder.all(color: cs.outline),
        tableHeadAlign: TextAlign.center,
        horizontalRuleDecoration: BoxDecoration(
          border: Border(bottom: BorderSide(color: cs.outline, width: 1)),
        ),
        a: AppTextStyles.body.copyWith(
          color: cs.primary,
          decoration: TextDecoration.underline,
          decorationColor: cs.primary,
        ),
        listIndent: 24,
        listBullet:
            AppTextStyles.body.copyWith(color: cs.primary, height: 1.7),
      );

  // ── Focus Mode ────────────────────────────────────────────────────────────

  Widget _buildFocusExitButton(ColorScheme cs) => Positioned(
        top: 12,
        right: 12,
        child: GestureDetector(
          onTap: _toggleFocusMode,
          child: Container(
            padding:
                const EdgeInsets.symmetric(horizontal: 14, vertical: 8),
            decoration: BoxDecoration(
              color: cs.onSurface.withOpacity(0.07),
              borderRadius: BorderRadius.circular(20),
            ),
            child: Row(
              mainAxisSize: MainAxisSize.min,
              children: [
                Icon(Icons.fullscreen_exit_rounded,
                    size: 16, color: cs.onSurfaceVariant),
                const SizedBox(width: 6),
                Text(
                  'Focus',
                  style: AppTextStyles.caption.copyWith(
                    color: cs.onSurfaceVariant,
                    fontWeight: FontWeight.w600,
                  ),
                ),
              ],
            ),
          ),
        ),
      );
}

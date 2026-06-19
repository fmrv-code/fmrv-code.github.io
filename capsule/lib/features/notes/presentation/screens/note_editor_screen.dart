import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';

import '../../../../../core/providers/database_provider.dart';
import '../../../../../core/theme/app_text_styles.dart';
import '../../../../../data/datasources/app_database.dart';

class NoteEditorScreen extends ConsumerStatefulWidget {
  const NoteEditorScreen({super.key, this.noteId});

  /// null → nouvelle note, int → édition
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
  bool _saving = false;

  @override
  void initState() {
    super.initState();
    if (widget.noteId != null) _loadNote();
  }

  Future<void> _loadNote() async {
    final note =
        await ref.read(noteRepositoryProvider).getById(widget.noteId!);
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

    // Discard si entièrement vide
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
    if (mounted) context.pop();
  }

  void _toggleFocusMode() {
    setState(() => _focusMode = !_focusMode);

    if (_focusMode) {
      SystemChrome.setEnabledSystemUIMode(SystemUiMode.immersiveSticky);
      _contentFocus.requestFocus();
    } else {
      SystemChrome.setEnabledSystemUIMode(SystemUiMode.edgeToEdge);
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
          child: Stack(
            children: [
              _buildEditor(cs),
              if (_focusMode) _buildFocusExitButton(cs),
            ],
          ),
        ),
      ),
    );
  }

  PreferredSizeWidget _buildAppBar(ColorScheme cs) {
    return AppBar(
      backgroundColor: cs.surface,
      scrolledUnderElevation: 0,
      leading: IconButton(
        icon: const Icon(Icons.arrow_back_ios_new_rounded),
        onPressed: _save,
        tooltip: 'Sauvegarder et fermer',
      ),
      actions: [
        // Focus Mode
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
                  child: CircularProgressIndicator.adaptive(strokeWidth: 2),
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
  }

  Widget _buildEditor(ColorScheme cs) {
    return SingleChildScrollView(
      padding: const EdgeInsets.fromLTRB(24, 16, 24, 48),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          // Titre
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
                color: cs.onSurfaceVariant.withOpacity(0.5),
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

          // Contenu Markdown
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
                color: cs.onSurfaceVariant.withOpacity(0.5),
                height: 1.65,
              ),
              border: InputBorder.none,
              enabledBorder: InputBorder.none,
              focusedBorder: InputBorder.none,
              filled: false,
              contentPadding: EdgeInsets.zero,
            ),
            maxLines: null,
            keyboardType: TextInputType.multiline,
            textCapitalization: TextCapitalization.sentences,
            minLines: 20,
          ),
        ],
      ),
    );
  }

  Widget _buildFocusExitButton(ColorScheme cs) {
    return Positioned(
      top: 12,
      right: 12,
      child: AnimatedOpacity(
        opacity: _focusMode ? 1.0 : 0.0,
        duration: const Duration(milliseconds: 300),
        child: GestureDetector(
          onTap: _toggleFocusMode,
          child: Container(
            padding: const EdgeInsets.symmetric(horizontal: 14, vertical: 8),
            decoration: BoxDecoration(
              color: cs.onSurface.withOpacity(0.08),
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
      ),
    );
  }
}

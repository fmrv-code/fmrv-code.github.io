import 'package:flutter/material.dart';

import '../../../../../core/theme/app_text_styles.dart';

class MarkdownToolbar extends StatelessWidget {
  const MarkdownToolbar({
    super.key,
    required this.controller,
    required this.focusNode,
  });

  final TextEditingController controller;
  final FocusNode focusNode;

  // ── Helpers ───────────────────────────────────────────────────────────────

  /// Entoure la sélection (ou le curseur) avec prefix/suffix.
  void _wrap(String prefix, [String? suffix]) {
    final sfx = suffix ?? prefix;
    final value = controller.value;
    final sel = value.selection;
    if (!sel.isValid) return;

    final text = value.text;
    final selected = sel.textInside(text);

    // Toggle : si déjà entouré, on retire
    if (selected.isNotEmpty &&
        selected.startsWith(prefix) &&
        selected.endsWith(sfx)) {
      final inner = selected.substring(
          prefix.length, selected.length - sfx.length);
      final newText = text.replaceRange(sel.start, sel.end, inner);
      controller.value = TextEditingValue(
        text: newText,
        selection: TextSelection(
            baseOffset: sel.start, extentOffset: sel.start + inner.length),
      );
      focusNode.requestFocus();
      return;
    }

    final replacement = '$prefix$selected$sfx';
    final newText = text.replaceRange(sel.start, sel.end, replacement);
    final newOffset = selected.isEmpty
        ? sel.start + prefix.length
        : sel.start + replacement.length;

    controller.value = TextEditingValue(
      text: newText,
      selection: TextSelection.collapsed(offset: newOffset),
    );
    focusNode.requestFocus();
  }

  /// Ajoute/retire un préfixe en début de la ligne courante.
  void _linePrefix(String prefix) {
    final text = controller.text;
    final sel = controller.selection;
    if (!sel.isValid) return;

    int lineStart = sel.baseOffset;
    while (lineStart > 0 && text[lineStart - 1] != '\n') {
      lineStart--;
    }

    final lineText = text.substring(lineStart);

    if (lineText.startsWith(prefix)) {
      // Toggle : retire le préfixe
      final newText = text.replaceRange(lineStart, lineStart + prefix.length, '');
      controller.value = TextEditingValue(
        text: newText,
        selection: TextSelection.collapsed(
          offset: (sel.baseOffset - prefix.length).clamp(lineStart, newText.length),
        ),
      );
    } else {
      // Remplace d'éventuels autres préfixes de titre avant d'ajouter
      final newText = text.replaceRange(lineStart, lineStart, prefix);
      controller.value = TextEditingValue(
        text: newText,
        selection: TextSelection.collapsed(
          offset: sel.baseOffset + prefix.length,
        ),
      );
    }
    focusNode.requestFocus();
  }

  // ── Build ─────────────────────────────────────────────────────────────────

  @override
  Widget build(BuildContext context) {
    final cs = Theme.of(context).colorScheme;

    return Container(
      height: 48,
      decoration: BoxDecoration(
        color: cs.surface,
        border: Border(top: BorderSide(color: cs.outline, width: 1)),
      ),
      child: SingleChildScrollView(
        scrollDirection: Axis.horizontal,
        padding: const EdgeInsets.symmetric(horizontal: 4),
        child: Row(
          children: [
            // Formatage texte
            _TxtBtn('B', bold: true, onTap: () => _wrap('**')),
            _TxtBtn('I', italic: true, onTap: () => _wrap('*')),
            _TxtBtn('S', strike: true, onTap: () => _wrap('~~')),
            _Sep(),
            // Titres (= taille de police)
            _TxtBtn('H1', onTap: () => _linePrefix('# ')),
            _TxtBtn('H2', onTap: () => _linePrefix('## ')),
            _TxtBtn('H3', onTap: () => _linePrefix('### ')),
            _Sep(),
            // Listes & blocs
            _IcnBtn(Icons.format_list_bulleted_rounded, 'Liste',
                () => _linePrefix('- ')),
            _IcnBtn(Icons.check_box_outline_blank_rounded, 'Case à cocher',
                () => _linePrefix('- [ ] ')),
            _IcnBtn(Icons.format_quote_rounded, 'Citation',
                () => _linePrefix('> ')),
            _Sep(),
            // Code & liens
            _IcnBtn(Icons.code_rounded, 'Code', () => _wrap('`')),
            _IcnBtn(Icons.link_rounded, 'Lien', () => _wrap('[', '](url)')),
          ],
        ),
      ),
    );
  }
}

// ── Boutons internes ──────────────────────────────────────────────────────────

class _TxtBtn extends StatelessWidget {
  const _TxtBtn(
    this.label, {
    required this.onTap,
    this.bold = false,
    this.italic = false,
    this.strike = false,
  });

  final String label;
  final VoidCallback onTap;
  final bool bold;
  final bool italic;
  final bool strike;

  @override
  Widget build(BuildContext context) {
    final cs = Theme.of(context).colorScheme;
    return InkWell(
      onTap: onTap,
      borderRadius: BorderRadius.circular(8),
      child: SizedBox(
        width: 40,
        height: 48,
        child: Center(
          child: Text(
            label,
            style: AppTextStyles.callout.copyWith(
              color: cs.onSurface,
              fontWeight: bold ? FontWeight.w700 : FontWeight.w500,
              fontStyle: italic ? FontStyle.italic : FontStyle.normal,
              decoration: strike ? TextDecoration.lineThrough : null,
              fontSize: label.length > 2 ? 13 : 15,
            ),
          ),
        ),
      ),
    );
  }
}

class _IcnBtn extends StatelessWidget {
  const _IcnBtn(this.icon, this.tooltip, this.onTap);
  final IconData icon;
  final String tooltip;
  final VoidCallback onTap;

  @override
  Widget build(BuildContext context) {
    final cs = Theme.of(context).colorScheme;
    return Tooltip(
      message: tooltip,
      child: InkWell(
        onTap: onTap,
        borderRadius: BorderRadius.circular(8),
        child: SizedBox(
          width: 44,
          height: 48,
          child: Icon(icon, size: 20, color: cs.onSurface),
        ),
      ),
    );
  }
}

class _Sep extends StatelessWidget {
  @override
  Widget build(BuildContext context) => Container(
        width: 1,
        height: 24,
        margin: const EdgeInsets.symmetric(horizontal: 4),
        color: Theme.of(context).colorScheme.outline,
      );
}

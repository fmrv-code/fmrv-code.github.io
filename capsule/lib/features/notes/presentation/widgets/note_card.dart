import 'package:flutter/material.dart';
import 'package:intl/intl.dart';

import '../../../../../core/theme/app_text_styles.dart';
import '../../../../../data/datasources/app_database.dart';
import '../../../../../data/models/note.dart';

class NoteCard extends StatelessWidget {
  const NoteCard({
    super.key,
    required this.note,
    required this.onTap,
  });

  final Note note;
  final VoidCallback onTap;

  @override
  Widget build(BuildContext context) {
    final cs = Theme.of(context).colorScheme;

    return GestureDetector(
      onTap: onTap,
      behavior: HitTestBehavior.opaque,
      child: Container(
        decoration: BoxDecoration(
          color: cs.surface,
          borderRadius: BorderRadius.circular(20),
          border: Border.all(color: cs.outline, width: 1),
        ),
        padding: const EdgeInsets.all(20),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            // Pin indicator
            if (note.isPinned) ...[
              Row(
                children: [
                  Icon(Icons.push_pin_rounded, size: 13, color: cs.primary),
                  const SizedBox(width: 4),
                  Text(
                    'Épinglée',
                    style: AppTextStyles.caption2.copyWith(color: cs.primary),
                  ),
                ],
              ),
              const SizedBox(height: 6),
            ],

            // Title
            Text(
              note.title.isEmpty ? 'Sans titre' : note.title,
              style: AppTextStyles.headline.copyWith(color: cs.onSurface),
              maxLines: 1,
              overflow: TextOverflow.ellipsis,
            ),

            // Excerpt
            if (note.excerpt.isNotEmpty) ...[
              const SizedBox(height: 5),
              Text(
                note.excerpt,
                style: AppTextStyles.subheadline.copyWith(
                  color: cs.onSurfaceVariant,
                ),
                maxLines: 2,
                overflow: TextOverflow.ellipsis,
              ),
            ],

            const SizedBox(height: 14),

            // Date
            Text(
              _formatDate(note.updatedAt),
              style: AppTextStyles.caption.copyWith(
                color: cs.onSurfaceVariant,
              ),
            ),
          ],
        ),
      ),
    );
  }

  String _formatDate(DateTime date) {
    final now = DateTime.now();
    final today = DateTime(now.year, now.month, now.day);
    final d = DateTime(date.year, date.month, date.day);
    final diff = today.difference(d).inDays;

    if (diff == 0) return 'Aujourd\'hui';
    if (diff == 1) return 'Hier';
    if (diff < 7) return DateFormat('EEEE', 'fr_FR').format(date);
    if (date.year == now.year) return DateFormat('d MMM', 'fr_FR').format(date);
    return DateFormat('d MMM y', 'fr_FR').format(date);
  }
}

import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';

import '../../../../../core/theme/app_colors.dart';
import '../../../../../core/theme/app_text_styles.dart';
import '../../providers/notes_provider.dart';
import '../widgets/note_card.dart';

class NotesScreen extends ConsumerStatefulWidget {
  const NotesScreen({super.key});

  @override
  ConsumerState<NotesScreen> createState() => _NotesScreenState();
}

class _NotesScreenState extends ConsumerState<NotesScreen> {
  final _searchCtrl = TextEditingController();

  @override
  void dispose() {
    _searchCtrl.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    final cs = Theme.of(context).colorScheme;
    final isDark = Theme.of(context).brightness == Brightness.dark;
    final notesAsync = ref.watch(filteredNotesProvider);

    return Scaffold(
      body: CustomScrollView(
        slivers: [
          // ── Grand titre iOS-style ──────────────────────────────────────────
          SliverAppBar.large(
            title: const Text('Capsule'),
            titleTextStyle: AppTextStyles.largeTitle.copyWith(
              color: cs.onSurface,
              fontWeight: FontWeight.w700,
            ),
          ),

          // ── Barre de recherche ────────────────────────────────────────────
          SliverToBoxAdapter(
            child: Padding(
              padding: const EdgeInsets.fromLTRB(20, 0, 20, 12),
              child: TextField(
                controller: _searchCtrl,
                onChanged: (v) =>
                    ref.read(notesSearchQueryProvider.notifier).state = v,
                decoration: InputDecoration(
                  hintText: 'Rechercher dans les notes…',
                  prefixIcon: Icon(
                    Icons.search,
                    color: cs.onSurfaceVariant,
                    size: 20,
                  ),
                  suffixIcon: _searchCtrl.text.isNotEmpty
                      ? IconButton(
                          icon: Icon(Icons.clear,
                              size: 18, color: cs.onSurfaceVariant),
                          onPressed: () {
                            _searchCtrl.clear();
                            ref.read(notesSearchQueryProvider.notifier).state =
                                '';
                          },
                        )
                      : null,
                ),
              ),
            ),
          ),

          // ── Contenu ───────────────────────────────────────────────────────
          notesAsync.when(
            loading: () => const SliverFillRemaining(
              child: Center(child: CircularProgressIndicator.adaptive()),
            ),
            error: (e, _) => SliverFillRemaining(
              child: Center(child: Text('Erreur : $e')),
            ),
            data: (notes) {
              if (notes.isEmpty) {
                return SliverFillRemaining(
                  child: _EmptyState(isSearch: _searchCtrl.text.isNotEmpty),
                );
              }

              return SliverPadding(
                padding: const EdgeInsets.fromLTRB(20, 4, 20, 120),
                sliver: SliverList.separated(
                  itemCount: notes.length,
                  separatorBuilder: (_, __) => const SizedBox(height: 12),
                  itemBuilder: (context, i) => NoteCard(
                    note: notes[i],
                    onTap: () => context.push('/notes/${notes[i].id}'),
                  ),
                ),
              );
            },
          ),
        ],
      ),

      floatingActionButton: FloatingActionButton.extended(
        onPressed: () => context.push('/notes/new'),
        icon: const Icon(Icons.add),
        label: const Text('Nouvelle note'),
      ),
    );
  }
}

class _EmptyState extends StatelessWidget {
  const _EmptyState({required this.isSearch});
  final bool isSearch;

  @override
  Widget build(BuildContext context) {
    final cs = Theme.of(context).colorScheme;
    final isDark = Theme.of(context).brightness == Brightness.dark;

    return Center(
      child: Padding(
        padding: const EdgeInsets.symmetric(horizontal: 48),
        child: Column(
          mainAxisSize: MainAxisSize.min,
          children: [
            Container(
              width: 80,
              height: 80,
              decoration: BoxDecoration(
                color: isDark
                    ? AppColors.darkSurfaceVariant
                    : AppColors.lightSurfaceVariant,
                shape: BoxShape.circle,
              ),
              child: Icon(
                isSearch ? Icons.search_off_rounded : Icons.article_outlined,
                size: 36,
                color: cs.primary,
              ),
            ),
            const SizedBox(height: 24),
            Text(
              isSearch ? 'Aucun résultat' : 'Aucune note',
              style: AppTextStyles.title3.copyWith(color: cs.onSurface),
              textAlign: TextAlign.center,
            ),
            const SizedBox(height: 8),
            Text(
              isSearch
                  ? 'Essaie avec d\'autres mots-clés.'
                  : 'Tes idées, citations et réflexions\nvivront ici — entièrement en local.',
              style: AppTextStyles.callout.copyWith(
                  color: cs.onSurfaceVariant),
              textAlign: TextAlign.center,
            ),
          ],
        ),
      ),
    );
  }
}

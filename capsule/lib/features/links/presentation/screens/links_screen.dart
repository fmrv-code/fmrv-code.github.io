import 'package:flutter/material.dart';

import '../../../../../core/theme/app_colors.dart';
import '../../../../../core/theme/app_text_styles.dart';

class LinksScreen extends StatelessWidget {
  const LinksScreen({super.key});

  @override
  Widget build(BuildContext context) {
    final cs = Theme.of(context).colorScheme;

    return Scaffold(
      body: CustomScrollView(
        slivers: [
          SliverAppBar.large(
            title: const Text('Liens'),
            titleTextStyle: AppTextStyles.largeTitle.copyWith(
              color: cs.onSurface,
              fontWeight: FontWeight.w700,
            ),
            actions: [
              IconButton(
                icon: const Icon(Icons.search),
                tooltip: 'Rechercher',
                onPressed: () {},
              ),
              const SizedBox(width: 8),
            ],
          ),
          const SliverFillRemaining(
            child: _EmptyLinksState(),
          ),
        ],
      ),
      floatingActionButton: FloatingActionButton.extended(
        onPressed: () {},
        icon: const Icon(Icons.add_link),
        label: const Text('Ajouter un lien'),
      ),
    );
  }
}

class _EmptyLinksState extends StatelessWidget {
  const _EmptyLinksState();

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
              width: 88,
              height: 88,
              decoration: BoxDecoration(
                color: isDark
                    ? AppColors.darkSurfaceVariant
                    : AppColors.lightSurfaceVariant,
                shape: BoxShape.circle,
              ),
              child: Icon(
                Icons.link_outlined,
                size: 40,
                color: cs.primary,
              ),
            ),
            const SizedBox(height: 24),
            Text(
              'Aucun lien sauvegardé',
              style: AppTextStyles.title3.copyWith(color: cs.onSurface),
              textAlign: TextAlign.center,
            ),
            const SizedBox(height: 8),
            Text(
              'Colle une URL et Capsule génère\nune belle carte automatiquement.',
              style: AppTextStyles.callout.copyWith(
                color: cs.onSurfaceVariant,
              ),
              textAlign: TextAlign.center,
            ),
          ],
        ),
      ),
    );
  }
}

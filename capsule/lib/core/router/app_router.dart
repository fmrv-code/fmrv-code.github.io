import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';

import '../../features/links/presentation/screens/links_screen.dart';
import '../../features/notes/presentation/screens/note_editor_screen.dart';
import '../../features/notes/presentation/screens/notes_screen.dart';
import '../../features/settings/presentation/screens/settings_screen.dart';
import '../widgets/app_shell.dart';

final routerProvider = Provider<GoRouter>((ref) => _router);

final _router = GoRouter(
  initialLocation: '/notes',
  debugLogDiagnostics: false,
  routes: [
    StatefulShellRoute.indexedStack(
      builder: (context, state, shell) => AppShell(shell: shell),
      branches: [
        // ── Notes ────────────────────────────────────────────────────────────
        StatefulShellBranch(
          routes: [
            GoRoute(
              path: '/notes',
              builder: (context, state) => const NotesScreen(),
              routes: [
                GoRoute(
                  path: 'new',
                  pageBuilder: (context, state) => _slideUpPage(
                    const NoteEditorScreen(),
                  ),
                ),
                GoRoute(
                  path: ':id',
                  pageBuilder: (context, state) {
                    final id = int.tryParse(
                          state.pathParameters['id'] ?? '',
                        ) ??
                        0;
                    return _slideUpPage(NoteEditorScreen(noteId: id));
                  },
                ),
              ],
            ),
          ],
        ),

        // ── Liens ─────────────────────────────────────────────────────────────
        StatefulShellBranch(
          routes: [
            GoRoute(
              path: '/links',
              builder: (context, state) => const LinksScreen(),
            ),
          ],
        ),

        // ── Réglages ─────────────────────────────────────────────────────────
        StatefulShellBranch(
          routes: [
            GoRoute(
              path: '/settings',
              builder: (context, state) => const SettingsScreen(),
            ),
          ],
        ),
      ],
    ),
  ],
);

CustomTransitionPage<void> _slideUpPage(Widget child) {
  return CustomTransitionPage<void>(
    child: child,
    transitionsBuilder: (context, animation, secondaryAnimation, child) {
      return SlideTransition(
        position: animation.drive(
          Tween(
            begin: const Offset(0, 0.06),
            end: Offset.zero,
          ).chain(CurveTween(curve: Curves.easeOutCubic)),
        ),
        child: FadeTransition(
          opacity: animation.drive(
            CurveTween(curve: const Interval(0, 0.6)),
          ),
          child: child,
        ),
      );
    },
  );
}

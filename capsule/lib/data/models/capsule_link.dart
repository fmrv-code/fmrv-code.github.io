import '../datasources/app_database.dart';

extension CapsuleLinkX on CapsuleLink {
  String get displayTitle => (title?.isNotEmpty == true) ? title! : url;

  String get domain {
    try {
      return Uri.parse(url).host.replaceFirst('www.', '');
    } catch (_) {
      return url;
    }
  }
}

CapsuleLink blankLink(String url) => CapsuleLink(
      id: 0,
      url: url,
      title: null,
      description: null,
      faviconUrl: null,
      imageUrl: null,
      siteName: null,
      metadataFetched: false,
      capsuleId: null,
      tagIds: const [],
      isFavorite: false,
      createdAt: DateTime.now(),
      updatedAt: DateTime.now(),
    );

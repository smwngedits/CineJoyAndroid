import SwiftUI
import WebKit

struct ContentView: View {
    @State private var progress: Double = 0
    @State private var isLoading = true
    @State private var reloadToken = UUID()

    var body: some View {
        ZStack {
            Color.black.ignoresSafeArea()

            CineJoyWebView(
                progress: $progress,
                isLoading: $isLoading,
                reloadToken: reloadToken
            )
            .ignoresSafeArea()

            if isLoading {
                VStack(spacing: 18) {
                    Image("CineJoyLogo")
                        .resizable()
                        .aspectRatio(contentMode: .fit)
                        .frame(width: 148, height: 148)

                    ProgressView(value: min(max(progress, 0), 1))
                        .progressViewStyle(.linear)
                        .tint(.white)
                        .frame(width: 164)
                }
                .frame(maxWidth: .infinity, maxHeight: .infinity)
                .background(Color.black)
                .ignoresSafeArea()
                .transition(.opacity)
                .zIndex(2)
            }
        }
        .background(Color.black)
        .statusBarHidden(false)
    }
}

private struct CineJoyWebView: UIViewRepresentable {
    @Binding var progress: Double
    @Binding var isLoading: Bool
    let reloadToken: UUID

    func makeCoordinator() -> Coordinator {
        Coordinator(self)
    }

    func makeUIView(context: Context) -> WKWebView {
        let configuration = WKWebViewConfiguration()
        configuration.allowsInlineMediaPlayback = true
        configuration.mediaTypesRequiringUserActionForPlayback = []

        let webView = WKWebView(frame: .zero, configuration: configuration)
        webView.navigationDelegate = context.coordinator
        webView.uiDelegate = context.coordinator
        webView.isOpaque = false
        webView.backgroundColor = .black
        webView.scrollView.backgroundColor = .black
        webView.scrollView.contentInsetAdjustmentBehavior = .never
        webView.allowsBackForwardNavigationGestures = true
        webView.configuration.preferences.javaScriptCanOpenWindowsAutomatically = false

        context.coordinator.observeProgress(of: webView)
        webView.load(URLRequest(url: URL(string: "https://cinejoy.pk/")!,
                                cachePolicy: .useProtocolCachePolicy,
                                timeoutInterval: 30))
        return webView
    }

    func updateUIView(_ webView: WKWebView, context: Context) {
        if context.coordinator.lastReloadToken != reloadToken {
            context.coordinator.lastReloadToken = reloadToken
            isLoading = true
            progress = 0
            webView.load(URLRequest(url: URL(string: "https://cinejoy.pk/")!))
        }
    }

    static func dismantleUIView(_ webView: WKWebView, coordinator: Coordinator) {
        coordinator.stopObserving()
        webView.stopLoading()
    }

    final class Coordinator: NSObject, WKNavigationDelegate, WKUIDelegate {
        private var parent: CineJoyWebView
        private var progressObservation: NSKeyValueObservation?
        private var contentCheckCount = 0
        var lastReloadToken: UUID?

        init(_ parent: CineJoyWebView) {
            self.parent = parent
        }

        func observeProgress(of webView: WKWebView) {
            progressObservation = webView.observe(\.estimatedProgress, options: [.new]) { [weak self] webView, _ in
                DispatchQueue.main.async {
                    self?.parent.progress = webView.estimatedProgress
                }
            }
        }

        func stopObserving() {
            progressObservation?.invalidate()
            progressObservation = nil
        }

        func webView(_ webView: WKWebView, didFinish navigation: WKNavigation!) {
            contentCheckCount = 0
            waitForVisibleContent(in: webView)
        }

        func webView(_ webView: WKWebView, didFail navigation: WKNavigation!, withError error: Error) {
            finishLoading()
        }

        func webView(_ webView: WKWebView, didFailProvisionalNavigation navigation: WKNavigation!, withError error: Error) {
            finishLoading()
        }

        private func waitForVisibleContent(in webView: WKWebView) {
            contentCheckCount += 1
            let script = """
            (() => {
              const body = document.body;
              if (!body) return false;
              const text = (body.innerText || body.textContent || '').replace(/\\s+/g, ' ').trim();
              const height = Math.max(body.scrollHeight, document.documentElement.scrollHeight);
              return document.readyState === 'complete' && text.length > 35 && height > 120;
            })();
            """
            webView.evaluateJavaScript(script) { [weak self] result, _ in
                guard let self else { return }
                let ready = (result as? Bool) == true
                if ready || self.contentCheckCount >= 50 {
                    self.finishLoading()
                } else {
                    DispatchQueue.main.asyncAfter(deadline: .now() + 0.35) {
                        self.waitForVisibleContent(in: webView)
                    }
                }
            }
        }

        private func finishLoading() {
            DispatchQueue.main.async {
                self.parent.progress = 1
                withAnimation(.easeOut(duration: 0.2)) {
                    self.parent.isLoading = false
                }
            }
        }

        func webView(_ webView: WKWebView, decidePolicyFor navigationAction: WKNavigationAction,
                     decisionHandler: @escaping (WKNavigationActionPolicy) -> Void) {
            guard let url = navigationAction.request.url else {
                decisionHandler(.cancel)
                return
            }
            if url.scheme == "https" || url.scheme == "http" {
                decisionHandler(.allow)
            } else {
                decisionHandler(.cancel)
            }
        }
    }
}

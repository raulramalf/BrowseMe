import { Navigate, Route, Routes } from 'react-router'
import { Layout } from './components/Layout'
import { AboutPage } from './pages/AboutPage'
import { AnalysisPage } from './pages/AnalysisPage'
import { AnalyzePage } from './pages/AnalyzePage'
import { CookiesPage } from './pages/CookiesPage'
import { DownloadPage } from './pages/DownloadPage'
import { HomePage } from './pages/HomePage'
import { NotFoundPage } from './pages/NotFoundPage'
import { PrivacyPage } from './pages/PrivacyPage'
import { TermsPage } from './pages/TermsPage'
import { isPublicSite } from './site'

export function App() {
  return (
    <Layout>
      <Routes>
        <Route path="/" element={<HomePage />} />
        {isPublicSite() ? (
          <>
            {/* La web informativa no tiene backend: el analizador está en la app. */}
            <Route path="/analizar" element={<Navigate to="/descargar" replace />} />
            <Route path="/analisis/:id" element={<Navigate to="/descargar" replace />} />
          </>
        ) : (
          <>
            <Route path="/analizar" element={<AnalyzePage />} />
            <Route path="/analisis/:id" element={<AnalysisPage />} />
          </>
        )}
        <Route path="/descargar" element={<DownloadPage />} />
        <Route path="/sobre" element={<AboutPage />} />
        <Route path="/privacidad" element={<PrivacyPage />} />
        <Route path="/terminos" element={<TermsPage />} />
        <Route path="/cookies" element={<CookiesPage />} />
        <Route path="*" element={<NotFoundPage />} />
      </Routes>
    </Layout>
  )
}

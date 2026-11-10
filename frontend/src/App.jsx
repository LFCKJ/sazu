import { useEffect, useMemo, useRef, useState } from 'react'
import { toBlob, toPng } from 'html-to-image'
import { QRCodeSVG } from 'qrcode.react'
import axios from 'axios'
import './App.css'

const BIRTH_TIME_OPTIONS = [
  { value: '사시 (09:30~11:29)', hour: 10, label: '사시 (巳時 · 09:30 ~ 11:29 · 10시)' },
  { value: '시간 모름', hour: null, label: '시간 모름 (출생시간 미상 · 시주 없음)' },
  { value: '자시 (23:30~01:29)', hour: 0, label: '자시 (子時 · 23:30 ~ 01:29)' },
  { value: '축시 (01:30~03:29)', hour: 2, label: '축시 (丑時 · 01:30 ~ 03:29)' },
  { value: '인시 (03:30~05:29)', hour: 4, label: '인시 (寅時 · 03:30 ~ 05:29)' },
  { value: '묘시 (05:30~07:29)', hour: 6, label: '묘시 (卯時 · 05:30 ~ 07:29)' },
  { value: '진시 (07:30~09:29)', hour: 8, label: '진시 (辰時 · 07:30 ~ 09:29)' },
  { value: '오시 (11:30~13:29)', hour: 12, label: '오시 (午時 · 11:30 ~ 13:29)' },
  { value: '미시 (13:30~15:29)', hour: 14, label: '미시 (未時 · 13:30 ~ 15:29)' },
  { value: '신시 (15:30~17:29)', hour: 16, label: '신시 (申時 · 15:30 ~ 17:29)' },
  { value: '유시 (17:30~19:29)', hour: 18, label: '유시 (酉時 · 17:30 ~ 19:29)' },
  { value: '술시 (19:30~21:29)', hour: 20, label: '술시 (戌時 · 19:30 ~ 21:29)' },
  { value: '해시 (21:30~23:29)', hour: 22, label: '해시 (亥時 · 21:30 ~ 23:29)' },
]

const CARD_THEMES = [
  {
    id: 'talisman',
    icon: '🧧',
    name: '황금 수호 부적카드',
    sub: '소원 성취 황금·주홍 부적 포토카드',
  },
  {
    id: 'photocard',
    icon: '🔮',
    name: '홀로그램 포토카드',
    sub: '다크 미드나잇 & 골드 프레임',
  },
  {
    id: 'receipt',
    icon: '🧾',
    name: '만세력 운세 영수증',
    sub: '레트로 감열지 영수증 스타일',
  },
]

const PRINT_PRESETS = [
  {
    id: 'photocard',
    name: '포토카드 정사이즈 (1매)',
    dim: '55 × 85 mm (신용카드 규격)',
    pageCss: '@page { size: 55mm 85mm; margin: 0; }',
  },
  {
    id: 'photo4x6-duo',
    name: '4×6 인화지 포토카드 2매 모아찍기',
    dim: '100 × 148 mm 용지에 55×85mm 2장',
    pageCss: '@page { size: 148mm 100mm landscape; margin: 0; }',
  },
  {
    id: 'photo4x6-single',
    name: '4×6 포토프린터 꽉 찬 1매',
    dim: '100 × 148 mm (포토프린터 표준)',
    pageCss: '@page { size: 100mm 148mm; margin: 0; }',
  },
  {
    id: 'receipt80',
    name: '80mm 감열 영수증 프린터',
    dim: '80mm 롤지 폭 (영수증 전용)',
    pageCss: '@page { size: 80mm auto; margin: 0; }',
  },
]

const ROMAN_NUMERALS = ['I', 'II', 'III', 'IV', 'V', 'VI', 'VII', 'VIII', 'IX', 'X', 'XI', 'XII']

function getDownloadUuidFromLocation() {
  const path = window.location.pathname
  if (path.startsWith('/download/')) {
    return path.replace('/download/', '').split('/')[0].trim()
  }
  const params = new URLSearchParams(window.location.search)
  return params.get('download') || params.get('uuid') || null
}

export default function App() {
  const initialDownloadUuid = useMemo(() => getDownloadUuidFromLocation(), [])
  const [downloadViewUuid, setDownloadViewUuid] = useState(initialDownloadUuid)

  if (downloadViewUuid) {
    return (
      <MobileDownloadPage
        uuid={downloadViewUuid}
        onGoHome={() => {
          window.history.pushState({}, '', '/')
          setDownloadViewUuid(null)
        }}
      />
    )
  }

  return <FortuneStudioApp />
}

/* ==========================================================================
   모바일 QR 스캔 시 열리는 전용 다운로드 뷰 (/download/:uuid)
   ========================================================================== */
function MobileDownloadPage({ uuid, onGoHome }) {
  const [fortune, setFortune] = useState(null)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState(null)

  const imageUrl = `/api/fortune/${uuid}/image`
  const directDownloadUrl = `/api/fortune/${uuid}/download`

  useEffect(() => {
    let active = true
    axios
      .get(`/api/fortune/${uuid}`)
      .then((res) => {
        if (active) {
          setFortune(res.data)
          setLoading(false)
        }
      })
      .catch(() => {
        if (active) {
          setError('운세 포토카드 정보를 불러오지 못했습니다. 만료되었거나 잘못된 QR 링크입니다.')
          setLoading(false)
        }
      })
    return () => {
      active = false
    }
  }, [uuid])

  return (
    <div className="mobile-download-shell">
      <div className="brand-badge">🔮 SAZU MOBILE DOWNLOAD</div>
      <h1 className="app-title" style={{ fontSize: '1.65rem' }}>
        나만의 사주 부적 포토카드
      </h1>
      <p className="app-subtitle" style={{ marginBottom: '16px' }}>
        아래 버튼을 누르거나 이미지를 길게 눌러 내 휴대폰 사진첩에 저장하세요!
      </p>

      {loading && (
        <div className="panel-card" style={{ padding: '40px 20px' }}>
          운세 포토카드 이미지를 불러오는 중입니다...
        </div>
      )}

      {error && (
        <div className="panel-card" style={{ padding: '28px 20px' }}>
          <p style={{ color: '#fca5a5', marginBottom: '16px' }}>{error}</p>
          <button className="primary-btn" onClick={onGoHome}>
            새 운세 포토카드 뽑으러 가기
          </button>
        </div>
      )}

      {!loading && !error && fortune && (
        <>
          <img
            src={imageUrl}
            alt="운세 포토카드"
            className="mobile-card-preview-img"
          />

          <div style={{ display: 'flex', flexDirection: 'column', gap: '10px', marginTop: '18px' }}>
            <a href={directDownloadUrl} download className="action-btn gold">
              📥 내 휴대폰에 고화질 PNG 저장하기
            </a>

            <button
              className="action-btn purple"
              onClick={() => window.print()}
            >
              🖨️ 모바일 프린터 / AirPrint로 인쇄하기
            </button>

            <button className="action-btn outline" onClick={onGoHome}>
              ✨ 나도 새로운 운세 포토카드 뽑기
            </button>
          </div>

          <p style={{ fontSize: '0.78rem', color: '#94a3b8', marginTop: '16px' }}>
            💡 아이폰(iOS) Safari에서는 위 포토카드 이미지를 1초간 길게 누른 뒤{' '}
            <strong>&ldquo;사진에 저장&rdquo;</strong>을 누르면 앨범에 바로 들어갑니다.
          </p>

          <div className="print-only-sheet print-size-photocard">
            <div className="print-sheet-inner">
              <div className="print-card-item">
                <img src={imageUrl} alt="인쇄용 포토카드" />
              </div>
            </div>
          </div>
        </>
      )}
    </div>
  )
}

/* ==========================================================================
   메인 운세 포토카드 & 영수증 스튜디오 (SAZU API 연동)
   ========================================================================== */
function FortuneStudioApp() {
  const [step, setStep] = useState(1)

  // Step 1 입력 상태 (기본값: SAZU API 샘플 1)
  const [name, setName] = useState('')
  const [birthDate, setBirthDate] = useState('1998-05-19')
  const [birthTime, setBirthTime] = useState('사시 (09:30~11:29)')
  const [gender, setGender] = useState('남성')
  const [styleTheme, setStyleTheme] = useState('talisman')
  const [wishMessage, setWishMessage] = useState('말하는 대로 다 이뤄지는 비비디 바비디~부')
  const [samples, setSamples] = useState([])
  const [selectedSampleId, setSelectedSampleId] = useState('strong-male')

  // Step 2 덱 상태
  const [deckOrder, setDeckOrder] = useState(() =>
    Array.from({ length: 12 }, (_, i) => i + 1)
  )
  const [isShuffling, setIsShuffling] = useState(false)
  const [pickedCardNum, setPickedCardNum] = useState(null)
  const [isLoadingFortune, setIsLoadingFortune] = useState(false)

  // Step 3 운세 결과 & 캡처/업로드/인쇄 상태
  const [fortune, setFortune] = useState(null)
  const [uploadStatus, setUploadStatus] = useState('idle') // idle | capturing | uploaded | error
  const [capturedDataUrl, setCapturedDataUrl] = useState(null)
  const [qrMode, setQrMode] = useState('page') // 'page' | 'direct'
  const [customHostOrigin, setCustomHostOrigin] = useState('')

  // 프린터 출력 옵션 상태
  const [printPreset, setPrintPreset] = useState('photocard')
  const [showCropMarks, setShowCropMarks] = useState(true)
  const [includeCardBack, setIncludeCardBack] = useState(true)

  const cardRef = useRef(null)

  // 초기 네트워크 LAN IP 및 SAZU API 샘플 프로필 목록 조회
  useEffect(() => {
    axios
      .get('/api/fortune/network-info')
      .then((res) => {
        const lanIp = res.data?.lanIp
        const port = window.location.port ? `:${window.location.port}` : ''
        if (lanIp && lanIp !== 'localhost' && window.location.hostname === 'localhost') {
          setCustomHostOrigin(`${window.location.protocol}//${lanIp}${port}`)
        } else {
          setCustomHostOrigin(window.location.origin)
        }
      })
      .catch(() => {
        setCustomHostOrigin(window.location.origin)
      })

    axios
      .get('/api/fortune/samples')
      .then((res) => {
        if (Array.isArray(res.data)) {
          setSamples(res.data)
        }
      })
      .catch(() => {})
  }, [])

  // 샘플 클릭 시 생년월일/시간/성별 자동 세팅
  const handleApplySample = (sample) => {
    setSelectedSampleId(sample.id)
    const inp = sample.input
    if (inp) {
      const y = String(inp.birthYear).padStart(4, '0')
      const m = String(inp.birthMonth).padStart(2, '0')
      const d = String(inp.birthDay).padStart(2, '0')
      setBirthDate(`${y}-${m}-${d}`)
      if (inp.birthHour === null || inp.birthHour === undefined) {
        setBirthTime('시간 모름')
      } else {
        setBirthTime('사시 (09:30~11:29)')
      }
      setGender(inp.isFemale ? '여성' : '남성')
    }
  }

  // QR코드에 담길 최종 URL 계산
  const qrTargetUrl = useMemo(() => {
    if (!fortune?.uuid) return ''
    const origin = (customHostOrigin || window.location.origin).replace(/\/$/, '')
    if (qrMode === 'direct') {
      return `${origin}/api/fortune/${fortune.uuid}/download`
    }
    return `${origin}/download/${fortune.uuid}`
  }, [fortune, customHostOrigin, qrMode])

  // 카드 섞기 애니메이션
  const handleShuffleDeck = () => {
    if (isShuffling || isLoadingFortune) return
    setIsShuffling(true)
    setPickedCardNum(null)
    setTimeout(() => {
      setDeckOrder((prev) => {
        const copy = [...prev]
        for (let i = copy.length - 1; i > 0; i--) {
          const j = Math.floor(Math.random() * (i + 1))
          ;[copy[i], copy[j]] = [copy[j], copy[i]]
        }
        return copy
      })
      setIsShuffling(false)
    }, 420)
  }

  // 카드 뽑기 클릭 -> 백엔드 SAZU API 연동 운세 생성 호출
  const handlePickCard = async (cardNumber) => {
    if (isLoadingFortune || pickedCardNum !== null) return
    setPickedCardNum(cardNumber)
    setIsLoadingFortune(true)

    const matchedTime = BIRTH_TIME_OPTIONS.find((o) => o.value === birthTime)

    try {
      const response = await axios.post('/api/fortune', {
        name: name.trim() || '행운의 주인공',
        birthDate,
        birthHour: matchedTime ? matchedTime.hour : 10,
        birthTime,
        gender,
        cardNumber,
        styleTheme,
        wishMessage: wishMessage.trim(),
      })

      setTimeout(() => {
        setFortune(response.data)
        setStep(3)
        setIsLoadingFortune(false)
      }, 480)
    } catch (err) {
      console.error('운세 생성 실패:', err)
      alert('운세 생성 중 오류가 발생했습니다. 백엔드 서버가 실행 중인지 확인해 주세요.')
      setPickedCardNum(null)
      setIsLoadingFortune(false)
    }
  }

  // Step 3 진입 또는 테마/QR URL 변경 시 카드 DOM을 캡처해 서버로 업로드
  const captureAndUploadCard = async () => {
    if (!cardRef.current || !fortune?.uuid) return
    setUploadStatus('capturing')

    try {
      await new Promise((r) => setTimeout(r, 180))

      const bgColor =
        styleTheme === 'receipt'
          ? '#ffffff'
          : styleTheme === 'talisman'
            ? '#f59e0b'
            : '#131129'

      const dataUrl = await toPng(cardRef.current, {
        pixelRatio: 3,
        cacheBust: true,
        backgroundColor: bgColor,
      })
      setCapturedDataUrl(dataUrl)

      const blob = await toBlob(cardRef.current, {
        pixelRatio: 3,
        cacheBust: true,
        backgroundColor: bgColor,
      })

      if (blob) {
        const formData = new FormData()
        formData.append('file', blob, `${fortune.uuid}.png`)
        await axios.post(`/api/fortune/${fortune.uuid}/image`, formData, {
          headers: { 'Content-Type': 'multipart/form-data' },
        })
        setUploadStatus('uploaded')
      }
    } catch (err) {
      console.error('카드 이미지 캡처/업로드 오류:', err)
      setUploadStatus('error')
    }
  }

  useEffect(() => {
    if (step === 3 && fortune?.uuid) {
      captureAndUploadCard()
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [step, fortune?.uuid, styleTheme, qrTargetUrl])

  // 내 기기에 PNG 이미지 파일 즉시 저장
  const handleDownloadLocalPng = async () => {
    if (!fortune) return
    try {
      let urlToSave = capturedDataUrl
      if (!urlToSave && cardRef.current) {
        urlToSave = await toPng(cardRef.current, { pixelRatio: 3, cacheBust: true })
        setCapturedDataUrl(urlToSave)
      }
      if (urlToSave) {
        const link = document.createElement('a')
        link.download = `sazu-${styleTheme}-${fortune.uuid.slice(0, 8)}.png`
        link.href = urlToSave
        link.click()
      }
    } catch (err) {
      console.error('로컬 이미지 저장 실패:', err)
    }
  }

  // 선택한 용지 규격으로 프린터 인쇄 실행
  const handlePrintCard = async () => {
    if (!capturedDataUrl && cardRef.current) {
      await captureAndUploadCard()
    }
    window.print()
  }

  const activePrintPreset =
    PRINT_PRESETS.find((p) => p.id === printPreset) || PRINT_PRESETS[0]

  return (
    <>
      <style>{`@media print { ${activePrintPreset.pageCss} }`}</style>

      {/* 일반 웹/키오스크 화면 (인쇄 시에는 숨김) */}
      <div className="app-shell no-print">
        <header className="app-header">
          <div className="brand-badge">
            <span>🔮 SAZU API 연동</span>
            <span>·</span>
            <span>QR & PHOTOCARD PRINTER</span>
          </div>
          <h1 className="app-title">데일리 사주 부적 포토카드 & 영수증 발급기</h1>
          <p className="app-subtitle">
            SAZU 만세력·오늘의 운세 API와 12신수 부적 카드를 결합해 QR로 폰에 저장하거나 포토카드로 즉시 인쇄하세요.
          </p>
        </header>

        {/* 단계 표시 바 */}
        <div className="stepper">
          <div className={`step-pill ${step === 1 ? 'active' : step > 1 ? 'done' : ''}`}>
            <span>01</span>
            <span>사주 명식 & 부적 문구</span>
          </div>
          <div className={`step-pill ${step === 2 ? 'active' : step > 2 ? 'done' : ''}`}>
            <span>02</span>
            <span>운세 카드 뽑기</span>
          </div>
          <div className={`step-pill ${step === 3 ? 'active' : ''}`}>
            <span>03</span>
            <span>QR 저장 & 포토카드 프린트</span>
          </div>
        </div>

        {/* ==================== STEP 1: 사주 입력 ==================== */}
        {step === 1 && (
          <section className="panel-card">
            <h2 className="panel-title">사주 명식 및 부적 소원 입력</h2>
            <p className="panel-desc">
              <code>api.sazu.app</code> 만세력·오늘의 운세 API를 통해 사주 4주팔자, 신강/신약, 일진 상호작용을 분석합니다.
            </p>

            {samples.length > 0 && (
              <div className="sample-presets-bar">
                <div className="sample-presets-title">
                  <span>⚡ SAZU API 공식 샘플 프로필 원클릭 불러오기</span>
                  <span style={{ fontSize: '0.72rem', color: '#94a3b8' }}>Free 샌드박스 공식 지원</span>
                </div>
                <div className="sample-chip-list">
                  {samples.map((s) => (
                    <button
                      key={s.id}
                      type="button"
                      className={`sample-chip-btn ${selectedSampleId === s.id ? 'active' : ''}`}
                      onClick={() => handleApplySample(s)}
                    >
                      {s.label}
                    </button>
                  ))}
                </div>
              </div>
            )}

            <form
              onSubmit={(e) => {
                e.preventDefault()
                handleShuffleDeck()
                setStep(2)
              }}
            >
              <div className="form-grid">
                <div className="form-group">
                  <label htmlFor="userName">이름 또는 닉네임 (선택)</label>
                  <input
                    id="userName"
                    type="text"
                    className="form-input"
                    placeholder="행운의 주인공"
                    value={name}
                    onChange={(e) => setName(e.target.value)}
                    maxLength={20}
                  />
                </div>

                <div className="form-group">
                  <label htmlFor="userGender">성별</label>
                  <select
                    id="userGender"
                    className="form-input"
                    value={gender}
                    onChange={(e) => setGender(e.target.value)}
                  >
                    <option value="남성">남성</option>
                    <option value="여성">여성</option>
                  </select>
                </div>

                <div className="form-group">
                  <label htmlFor="birthDate">생년월일 (양력)</label>
                  <input
                    id="birthDate"
                    type="date"
                    className="form-input"
                    value={birthDate}
                    onChange={(e) => {
                      setSelectedSampleId('')
                      setBirthDate(e.target.value)
                    }}
                    required
                  />
                </div>

                <div className="form-group">
                  <label htmlFor="birthTime">태어난 시간 (12시진)</label>
                  <select
                    id="birthTime"
                    className="form-input"
                    value={birthTime}
                    onChange={(e) => {
                      setSelectedSampleId('')
                      setBirthTime(e.target.value)
                    }}
                  >
                    {BIRTH_TIME_OPTIONS.map((opt) => (
                      <option key={opt.value} value={opt.value}>
                        {opt.label}
                      </option>
                    ))}
                  </select>
                </div>

                <div className="form-group full">
                  <label htmlFor="wishInput">부적/포토카드 상단 소원 문구 (자유 수정 가능)</label>
                  <input
                    id="wishInput"
                    type="text"
                    className="form-input"
                    placeholder="말하는 대로 다 이뤄지는 비비디 바비디~부"
                    value={wishMessage}
                    onChange={(e) => setWishMessage(e.target.value)}
                    maxLength={40}
                  />
                </div>

                <div className="form-group full">
                  <label>포토카드 / 부적 / 영수증 디자인 테마 선택 (발급 후에도 변경 가능)</label>
                  <div className="theme-selector">
                    {CARD_THEMES.map((t) => (
                      <div
                        key={t.id}
                        className={`theme-option ${styleTheme === t.id ? 'selected' : ''}`}
                        onClick={() => setStyleTheme(t.id)}
                      >
                        <span className="theme-option-icon">{t.icon}</span>
                        <span className="theme-option-name">{t.name}</span>
                        <span className="theme-option-sub">{t.sub}</span>
                      </div>
                    ))}
                  </div>
                </div>
              </div>

              <button type="submit" className="primary-btn">
                다음 단계 · 운세 카드 뽑으러 가기 →
              </button>
            </form>
          </section>
        )}

        {/* ==================== STEP 2: 12장 운세 카드 뽑기 ==================== */}
        {step === 2 && (
          <section className="deck-stage">
            <h2 className="panel-title">마음이 끌리는 수호 카드를 한 장 선택하세요</h2>
            <p className="panel-desc">
              카드를 터치하면 SAZU API 만세력·오늘의 운세 분석과 결합된 나만의 포토카드가 발급됩니다.
            </p>

            <div className="deck-toolbar">
              <button
                type="button"
                className="secondary-btn"
                onClick={() => setStep(1)}
                disabled={isLoadingFortune}
              >
                ← 정보 다시 입력
              </button>
              <button
                type="button"
                className="secondary-btn"
                onClick={handleShuffleDeck}
                disabled={isShuffling || isLoadingFortune}
              >
                🔀 카드 다시 섞기
              </button>
            </div>

            <div className="card-deck-grid">
              {deckOrder.map((cardNum, idx) => {
                const isPicked = pickedCardNum === cardNum
                return (
                  <div
                    key={`${cardNum}-${idx}`}
                    className={`tarot-card-slot ${isShuffling ? 'shuffling' : ''} ${
                      isPicked ? 'picked' : ''
                    }`}
                    onClick={() => handlePickCard(cardNum)}
                  >
                    <div className="tarot-back-top" style={{ position: 'relative', zIndex: 2 }}>
                      SAZU · {ROMAN_NUMERALS[idx]}
                    </div>
                    <div style={{ position: 'relative', zIndex: 2 }} />
                    <div className="tarot-back-bottom" style={{ position: 'relative', zIndex: 2 }}>
                      TOUCH TO DRAW
                    </div>
                  </div>
                )
              })}
            </div>
          </section>
        )}

        {/* ==================== STEP 3: 포토카드/영수증 발급 & QR/인쇄 스튜디오 ==================== */}
        {step === 3 && fortune && (
          <section className="studio-layout">
            {/* 좌측: 실제 캡처 및 인쇄되는 포토카드/영수증 프리뷰 */}
            <div className="card-preview-column">
              <div className="theme-QuickSwitch">
                {CARD_THEMES.map((t) => (
                  <button
                    key={t.id}
                    type="button"
                    className={`theme-chip ${styleTheme === t.id ? 'active' : ''}`}
                    onClick={() => setStyleTheme(t.id)}
                  >
                    {t.icon} {t.name}
                  </button>
                ))}
              </div>

              {/* html-to-image가 캡처하는 카드 DOM */}
              <div
                ref={cardRef}
                className={`fortune-card-canvas theme-${styleTheme}`}
              >
                <div>
                  <div className="fc-top-bar">
                    <span>SAZU · {fortune.serialCode}</span>
                    <span>{fortune.issuedAt}</span>
                  </div>

                  {/* 상단 소원 부적 문구 배너 */}
                  {fortune.wishMessage && (
                    <div className="fc-wish-banner">
                      ✨ {fortune.wishMessage} ✨
                    </div>
                  )}

                  <div className="fc-hero">
                    <div className="fc-symbol-badge">{fortune.cardSymbol}</div>
                    <div className="fc-hanja">{fortune.cardHanja}</div>
                    <h3 className="fc-card-title">
                      No.{String(fortune.cardNumber).padStart(2, '0')} {fortune.cardTitle}
                    </h3>
                    <div className="fc-keywords">
                      {fortune.cardKeywords?.map((kw) => (
                        <span key={kw} className="fc-keyword-tag">
                          #{kw}
                        </span>
                      ))}
                      {fortune.angelStars?.slice(0, 2).map((star) => (
                        <span key={star} className="fc-keyword-tag">
                          ★{star}
                        </span>
                      ))}
                    </div>
                  </div>

                  {/* SAZU API 사주 4주(시주·일주·월주·년주) 명식 테이블 */}
                  <div className="fc-pillars-table">
                    {[
                      fortune.hourPillar,
                      fortune.dayPillarInfo,
                      fortune.monthPillar,
                      fortune.yearPillar,
                    ].map(
                      (p, i) =>
                        p && (
                          <div key={i} className="fc-pillar-col">
                            <div className="fc-pillar-head">{p.label}</div>
                            <div className="fc-pillar-ganji">{p.full}</div>
                            <div className="fc-pillar-sub">
                              {p.sipseong} · {p.twelveStage}
                            </div>
                          </div>
                        )
                    )}
                  </div>

                  <div className="fc-user-strip">
                    <div>
                      <strong>{fortune.name}</strong> ({fortune.birthDate})
                    </div>
                    <div>
                      오늘 일진: <strong>{fortune.todayIlju}</strong> ({fortune.todaySipseong})
                    </div>
                  </div>

                  <div className="fc-scores-grid">
                    <div className="fc-score-box">
                      <div className="fc-score-label">재물운</div>
                      <div className="fc-score-val">{fortune.wealthScore}</div>
                    </div>
                    <div className="fc-score-box">
                      <div className="fc-score-label">연애/인연</div>
                      <div className="fc-score-val">{fortune.loveScore}</div>
                    </div>
                    <div className="fc-score-box">
                      <div className="fc-score-label">성취/학업</div>
                      <div className="fc-score-val">{fortune.careerScore}</div>
                    </div>
                    <div className="fc-score-box">
                      <div className="fc-score-label">종합점수</div>
                      <div className="fc-score-val">{fortune.overallScore}점</div>
                    </div>
                  </div>

                  <div className="fc-quote-box">
                    <div className="fc-quote-text">&ldquo;{fortune.summaryQuote}&rdquo;</div>
                    <div className="fc-detail-text">{fortune.detailMessage}</div>
                  </div>

                  <div className="fc-lucky-grid">
                    <div className="fc-lucky-item">
                      <span className="fc-lucky-key">사주강약</span>
                      <span className="fc-lucky-val">
                        {fortune.sinStrength} ({fortune.sinStrengthScore}점)
                      </span>
                    </div>
                    <div className="fc-lucky-item">
                      <span className="fc-lucky-key">행운컬러</span>
                      <span className="fc-lucky-val">{fortune.luckyColor}</span>
                    </div>
                    <div className="fc-lucky-item">
                      <span className="fc-lucky-key">행운아이템</span>
                      <span className="fc-lucky-val">{fortune.luckyItem}</span>
                    </div>
                    <div className="fc-lucky-item">
                      <span className="fc-lucky-key">행운방위</span>
                      <span className="fc-lucky-val">{fortune.luckyDirection}</span>
                    </div>
                  </div>
                </div>

                {/* 카드 하단 QR코드, 낙관 인장 및 시리얼 바코드 */}
                <div className="fc-footer">
                  <div style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
                    <div className="fc-red-stamp" title="소원성취 대길 인장">
                      <span>소</span>
                      <span>원</span>
                      <span>성</span>
                      <span>취</span>
                    </div>
                    <div className="fc-footer-left">
                      <span className="fc-serial">{fortune.serialCode}</span>
                      <span className="fc-qr-caption">
                        📱 QR 스캔 시 모바일 이미지 즉시 저장
                      </span>
                      <span className="fc-barcode-bars">||| |||| || ||||| ||| ||||</span>
                    </div>
                  </div>
                  <div className="fc-qr-frame">
                    <QRCodeSVG
                      value={qrTargetUrl || window.location.href}
                      size={64}
                      level="M"
                      includeMargin={false}
                    />
                  </div>
                </div>
              </div>
            </div>

            {/* 우측: QR 모바일 다운로드 & 이미지 파일 저장 & 포토카드 프린터 출력 스튜디오 */}
            <div className="studio-controls-column">
              {/* SAZU API 실시간 연동 상태 표시줄 */}
              <div
                className="control-box"
                style={{ padding: '14px 20px', display: 'flex', justifyContent: 'space-between', alignItems: 'center', flexWrap: 'wrap', gap: '8px' }}
              >
                <div style={{ fontSize: '0.84rem' }}>
                  <strong>🔗 SAZU API 명리 엔진 상태:</strong>{' '}
                  {fortune.sazuApiConnected ? (
                    <span style={{ color: '#6ee7b7' }}>
                      연동 성공 ({fortune.sazuApiTier?.toUpperCase()} 플랜)
                    </span>
                  ) : (
                    <span style={{ color: '#fde047' }}>오프라인 폴백</span>
                  )}
                </div>
                {fortune.appliedSampleLabel && (
                  <span style={{ fontSize: '0.75rem', color: '#94a3b8' }}>
                    적용 명식: {fortune.appliedSampleLabel}
                  </span>
                )}
              </div>

              {/* 1. QR코드 모바일 저장 박스 */}
              <div className="control-box">
                <div className="control-box-header">
                  <div className="control-box-title">
                    <span>📱 QR코드로 휴대폰에 즉시 저장</span>
                  </div>
                  {uploadStatus === 'uploaded' ? (
                    <span className="status-badge ready">✅ QR 이미지 서버 준비 완료</span>
                  ) : uploadStatus === 'capturing' ? (
                    <span className="status-badge working">⏳ 고해상도 이미지 캡처 중...</span>
                  ) : (
                    <span className="status-badge working">대기 중</span>
                  )}
                </div>

                <div className="qr-studio-body">
                  <div className="qr-big-frame">
                    <QRCodeSVG
                      value={qrTargetUrl || window.location.href}
                      size={132}
                      level="M"
                      includeMargin={false}
                    />
                  </div>

                  <div className="qr-studio-info">
                    <p style={{ fontSize: '0.86rem', color: '#cbd5e1' }}>
                      스마트폰 카메라로 좌측 카드의 QR 또는 이 QR코드를 스캔하면 포토카드 이미지를 내 폰에 바로 저장할 수 있습니다.
                    </p>

                    <div className="qr-mode-tabs">
                      <button
                        type="button"
                        className={`qr-mode-tab ${qrMode === 'page' ? 'active' : ''}`}
                        onClick={() => setQrMode('page')}
                      >
                        모바일 전용 뷰어 열기 (추천)
                      </button>
                      <button
                        type="button"
                        className={`qr-mode-tab ${qrMode === 'direct' ? 'active' : ''}`}
                        onClick={() => setQrMode('direct')}
                      >
                        스캔 즉시 파일 다운로드
                      </button>
                    </div>

                    <div className="host-input-row">
                      <input
                        type="text"
                        className="host-input"
                        value={customHostOrigin}
                        onChange={(e) => setCustomHostOrigin(e.target.value)}
                        placeholder="http://192.168.0.x:5173"
                        title="스마트폰과 같은 와이파이 사용 시 PC의 내부 IP 주소"
                      />
                    </div>
                    <span style={{ fontSize: '0.73rem', color: '#94a3b8' }}>
                      🔗 QR 연결 주소: <code>{qrTargetUrl}</code>
                    </span>
                  </div>
                </div>
              </div>

              {/* 2. 이미지 파일 직접 다운로드 박스 */}
              <div className="control-box">
                <div className="control-box-header">
                  <div className="control-box-title">
                    <span>💾 고화질 포토카드 이미지 저장</span>
                  </div>
                </div>

                <div className="action-btn-grid">
                  <button
                    type="button"
                    className="action-btn gold"
                    onClick={handleDownloadLocalPng}
                  >
                    📥 이 기기에 PNG 이미지 저장
                  </button>

                  <a
                    href={`/api/fortune/${fortune.uuid}/download`}
                    download
                    className="action-btn emerald"
                  >
                    ☁️ 서버 원본 PNG 다운로드
                  </a>
                </div>
              </div>

              {/* 3. 포토카드 / 영수증 프린터 인쇄 스튜디오 */}
              <div className="control-box">
                <div className="control-box-header">
                  <div className="control-box-title">
                    <span>🖨️ 포토카드 & 영수증 프린터 출력</span>
                  </div>
                  <span style={{ fontSize: '0.76rem', color: '#fde047' }}>
                    300DPI 고해상도 조판
                  </span>
                </div>

                <div className="print-preset-grid">
                  {PRINT_PRESETS.map((preset) => (
                    <button
                      key={preset.id}
                      type="button"
                      className={`print-preset-card ${
                        printPreset === preset.id ? 'selected' : ''
                      }`}
                      onClick={() => setPrintPreset(preset.id)}
                    >
                      <div className="print-preset-name">{preset.name}</div>
                      <div className="print-preset-dim">{preset.dim}</div>
                    </button>
                  ))}
                </div>

                <div className="print-options-row">
                  <label>
                    <input
                      type="checkbox"
                      checked={showCropMarks}
                      onChange={(e) => setShowCropMarks(e.target.checked)}
                    />
                    <span>오리기 편한 재단선(가이드라인) 표시</span>
                  </label>

                  <label>
                    <input
                      type="checkbox"
                      checked={includeCardBack}
                      onChange={(e) => setIncludeCardBack(e.target.checked)}
                    />
                    <span>포토카드 뒷면 커버 아트워크 함께 인쇄</span>
                  </label>
                </div>

                <div className="action-btn-grid">
                  <button
                    type="button"
                    className="action-btn purple"
                    style={{ gridColumn: '1 / -1' }}
                    onClick={handlePrintCard}
                  >
                    🖨️ 선택한 규격({activePrintPreset.name})으로 프린터 인쇄하기
                  </button>
                </div>
              </div>

              {/* 처음으로 / 다른 카드 뽑기 */}
              <div className="action-btn-grid">
                <button
                  type="button"
                  className="action-btn outline"
                  onClick={() => {
                    setPickedCardNum(null)
                    handleShuffleDeck()
                    setStep(2)
                  }}
                >
                  🃏 다른 운세 카드 다시 뽑기
                </button>
                <button
                  type="button"
                  className="action-btn outline"
                  onClick={() => {
                    setPickedCardNum(null)
                    setFortune(null)
                    setStep(1)
                  }}
                >
                  ✏️ 사주 명식 새로 입력하기
                </button>
              </div>
            </div>
          </section>
        )}
      </div>

      {/* ==================== 프린터 출력 전용 시트 (@media print 에서만 표시) ==================== */}
      {fortune && capturedDataUrl && (
        <div className={`print-only-sheet print-size-${printPreset}`}>
          <div className="print-sheet-inner">
            {/* 앞면 1매 */}
            <div className={`print-card-item ${showCropMarks ? 'with-crop-marks' : ''}`}>
              <img src={capturedDataUrl} alt="포토카드 앞면" />
            </div>

            {/* 4x6 2매 모아찍기 선택 시 두 번째 포토카드 배치 */}
            {printPreset === 'photo4x6-duo' && !includeCardBack && (
              <div className={`print-card-item ${showCropMarks ? 'with-crop-marks' : ''}`}>
                <img src={capturedDataUrl} alt="포토카드 앞면 2" />
              </div>
            )}

            {/* 뒷면 함께 인쇄 옵션 선택 시 업로드된 카드 뒷면 아트워크(/card-back.jpeg) 출력 */}
            {includeCardBack && (
              <div className={`print-card-item ${showCropMarks ? 'with-crop-marks' : ''}`}>
                <img src="/card-back.jpeg" alt="포토카드 뒷면 아트워크" />
              </div>
            )}
          </div>
        </div>
      )}
    </>
  )
}

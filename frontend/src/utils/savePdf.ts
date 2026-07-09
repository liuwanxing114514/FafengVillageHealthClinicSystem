export async function saveElementAsPdf(element: HTMLElement, filename: string): Promise<void> {
  const { default: html2pdf } = await import('html2pdf.js')
  const sheet = element.classList.contains('print-sheet') ? element : element.querySelector('.print-sheet')
  const target = (sheet ?? element) as HTMLElement

  await html2pdf()
    .set({
      margin: 0,
      filename,
      image: { type: 'jpeg', quality: 0.98 },
      html2canvas: {
        scale: 2,
        useCORS: true,
        logging: false,
        backgroundColor: '#ffffff',
      },
      jsPDF: { unit: 'mm', format: 'a4', orientation: 'portrait' },
      pagebreak: { mode: ['avoid-all'] },
    })
    .from(target)
    .save()
}

export function buildPrescriptionPdfFilename(
  patientName: string | undefined,
  prescriptionDate: string | undefined,
  prescriptionId: number,
): string {
  const safeName = (patientName ?? '患者').replace(/[/\\?%*:|"<>]/g, '_').trim() || '患者'
  const safeDate = (prescriptionDate ?? '').replace(/[/\\?%*:|"<>]/g, '-')
  const suffix = safeDate ? `-${safeDate}` : ''
  return `处方-${safeName}${suffix}-${prescriptionId}.pdf`
}

export function buildVisitPdfFilename(visitId: number, visitDate?: string): string {
  const safeDate = (visitDate ?? new Date().toISOString().slice(0, 10)).replace(/[/\\?%*:|"<>]/g, '-')
  return `病历-${visitId}-${safeDate}.pdf`
}

export function formatDisplayDate(value?: string | null): string {
  if (!value) return '—'
  return value.replace('T', ' ').slice(0, 16)
}

export function genderLabel(gender?: string | null): string {
  if (gender === 'M') return '男'
  if (gender === 'F') return '女'
  if (gender === 'UNKNOWN') return '未知'
  return gender ?? '—'
}

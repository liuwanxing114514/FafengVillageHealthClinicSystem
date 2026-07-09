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

// @ImagePlus imp1
// @ImagePlus imp2

// Colocalisation.groovy
//
// This script demonstrates programmatic usage of Fiji's Coloc 2 plugin,
// including how to extract quantitative measurements after execution.

import sc.fiji.coloc.Coloc_2
coloc2 = new Coloc_2()

indexMask = 0
indexRegr = 0
autoSavePdf = false
displayImages = false
displayShuffledCostes = false
useLiCh1 = true
useLiCh2 = true
useLiICQ = true
useSpearmanRank = true
useManders = true
useKendallTau = true
useScatterplot = true
useCostes = true
psf = 3
nrCostesRandomisations = 10

coloc2.initializeSettings(
    imp1,
    imp2,
    indexMask,
    indexRegr,
    autoSavePdf,
    displayImages,
    displayShuffledCostes,
    useLiCh1,
    useLiCh2,
    useLiICQ,
    useSpearmanRank,
    useManders,
    useKendallTau,
    useScatterplot,
    useCostes,
    psf,
    nrCostesRandomisations)

img1 = coloc2.img1
img2 = coloc2.img2
box = coloc2.masks[0].roi
mask = coloc2.masks[0].mask

// NB: Passing a different bounding box and/or mask here
// may work, but is (as of this writing) UNTESTED.
results = coloc2.colocalise(img1, img2, box, mask, null)
for (v in results.values()) {
  println(v.name + " = " + (v.isNumber ? v.number : v.value))
}
println("I also have histograms:")
for (h in results.histograms()) {
	println("\t" + h)
}
